package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payment.PaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.PaymentEntity;
import com.unionsg.xaccounting.entity.payment.PaymentRefundEntity;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentJournalServiceImpl implements PaymentJournalService {

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final com.unionsg.xaccounting.repository.payment.PaymentRepository paymentRepository;

    @Value("${payment.journal.bank-account-id}")
    private String bankAccountId;

    @Value("${payment.journal.accounts-receivable-account-id}")
    private String accountsReceivableAccountId;

    @Value("${payment.journal.customer-advances-account-id}")
    private String customerAdvancesAccountId;

    // ========================================================================
    // PAYMENT RECEIVED
    // ========================================================================

    @Override
    @Transactional
    public void postPaymentJournal(PaymentEntity payment) {
        checkNoExistingJournal(payment);

        Long bankAccountIdResolved = resolveAccountId(bankAccountId);
        Long arAccountIdResolved = resolveAccountId(accountsReceivableAccountId);
        Long advancesAccountIdResolved = resolveAccountId(customerAdvancesAccountId);

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Bank
        lines.add(CreateJournalLineRequest.builder()
                .accountId(bankAccountIdResolved)
                .description("Payment received: " + payment.getReceiptNumber())
                .debitAmount(payment.getAmountReceived())
                .creditAmount(BigDecimal.ZERO)
                .build());

        // Credit Accounts Receivable (allocated amount)
        if (payment.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(arAccountIdResolved)
                    .description("Allocated amount for " + payment.getReceiptNumber())
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(payment.getAllocatedAmount())
                    .build());
        }

        // Credit Customer Advances (unallocated amount)
        if (payment.getUnallocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(advancesAccountIdResolved)
                    .description("Unallocated balance for " + payment.getReceiptNumber())
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(payment.getUnallocatedAmount())
                    .build());
        }

        JournalResponse journalResponse = createAndPostJournal(payment, lines,
                "Receipt " + payment.getReceiptNumber() + " received from "
                        + (payment.getCustomer() != null ? payment.getCustomer().getDisplayName() : "Unknown")
                        + ".");

        // Track the journal on the payment
        payment.setJournalEntryId(journalResponse.getId());
        paymentRepository.save(payment);

        log.info("Payment journal posted for receipt: {}", payment.getReceiptNumber());
    }

    // ========================================================================
    // ADDITIONAL ALLOCATION
    // ========================================================================

    @Override
    @Transactional
    public void postAdditionalAllocationJournal(PaymentEntity payment, PaymentAllocationEntity allocation) {
        checkPaymentHasJournal(payment);

        Long arAccountIdResolved = resolveAccountId(accountsReceivableAccountId);
        Long advancesAccountIdResolved = resolveAccountId(customerAdvancesAccountId);

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Customer Advances
        lines.add(CreateJournalLineRequest.builder()
                .accountId(advancesAccountIdResolved)
                .description("Allocation of unallocated balance")
                .debitAmount(allocation.getAllocatedAmount())
                .creditAmount(BigDecimal.ZERO)
                .build());

        // Credit Accounts Receivable
        lines.add(CreateJournalLineRequest.builder()
                .accountId(arAccountIdResolved)
                .description("Additional allocation for invoice")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(allocation.getAllocatedAmount())
                .build());

        String description = "Additional allocation of " + allocation.getAllocatedAmount()
                + " for " + payment.getReceiptNumber() + ".";

        createAndPostJournal(payment, lines, description);

        log.info("Additional allocation journal posted for payment: {}", payment.getReceiptNumber());
    }

    // ========================================================================
    // REMOVE ALLOCATION
    // ========================================================================

    @Override
    @Transactional
    public void postRemoveAllocationJournal(PaymentEntity payment, PaymentAllocationEntity allocation) {
        checkPaymentHasJournal(payment);

        Long arAccountIdResolved = resolveAccountId(accountsReceivableAccountId);
        Long advancesAccountIdResolved = resolveAccountId(customerAdvancesAccountId);

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Debit Accounts Receivable (reverse the credit)
        lines.add(CreateJournalLineRequest.builder()
                .accountId(arAccountIdResolved)
                .description("Reversal of allocation")
                .debitAmount(allocation.getAllocatedAmount())
                .creditAmount(BigDecimal.ZERO)
                .build());

        // Credit Customer Advances
        lines.add(CreateJournalLineRequest.builder()
                .accountId(advancesAccountIdResolved)
                .description("Reversal of allocation")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(allocation.getAllocatedAmount())
                .build());

        String description = "Removal of allocation of " + allocation.getAllocatedAmount()
                + " for " + payment.getReceiptNumber() + ".";

        createAndPostJournal(payment, lines, description);

        log.info("Allocation removal journal posted for payment: {}", payment.getReceiptNumber());
    }

    // ========================================================================
    // REFUND
    // ========================================================================

    @Override
    @Transactional
    public void postRefundJournal(PaymentEntity payment, PaymentRefundEntity refund) {
        checkPaymentHasJournal(payment);

        Long bankAccountIdResolved = resolveAccountId(bankAccountId);
        Long arAccountIdResolved = resolveAccountId(accountsReceivableAccountId);
        Long advancesAccountIdResolved = resolveAccountId(customerAdvancesAccountId);

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        // Determine what to debit based on remaining allocated vs unallocated
        BigDecimal refundAmount = refund.getAmount();
        BigDecimal allocatedPortion = payment.getAllocatedAmount().min(refundAmount);
        BigDecimal unallocatedPortion = refundAmount.subtract(allocatedPortion);

        // Debit Accounts Receivable for the portion that was previously allocated
        if (allocatedPortion.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(arAccountIdResolved)
                    .description("Refund of allocated amount")
                    .debitAmount(allocatedPortion)
                    .creditAmount(BigDecimal.ZERO)
                    .build());
        }

        // Debit Customer Advances for the portion that was unallocated
        if (unallocatedPortion.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(advancesAccountIdResolved)
                    .description("Refund of unallocated amount")
                    .debitAmount(unallocatedPortion)
                    .creditAmount(BigDecimal.ZERO)
                    .build());
        }

        // Credit Bank
        lines.add(CreateJournalLineRequest.builder()
                .accountId(bankAccountIdResolved)
                .description("Refund: " + refund.getReferenceNumber())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(refundAmount)
                .build());

        String description = "Refund of " + refundAmount + " for " + payment.getReceiptNumber()
                + ". Reason: " + (refund.getReason() != null ? refund.getReason() : "N/A") + ".";

        createAndPostJournal(payment, lines, description);

        log.info("Refund journal posted for payment: {}", payment.getReceiptNumber());
    }

    // ========================================================================
    // CANCEL PAYMENT
    // ========================================================================

    @Override
    @Transactional
    public void postCancellationJournal(PaymentEntity payment) {
        checkPaymentHasJournal(payment);

        JournalEntry journal = journalEntryRepository
                .findById(payment.getJournalEntryId())
                .orElseThrow(() -> new BusinessException(
                        "Journal not found for payment: " + payment.getReceiptNumber()));

        // Use the existing JournalService.reverse() to create a reversing entry
        String reason = "Cancellation of payment " + payment.getReceiptNumber() + ".";
        journalService.reverse(journal.getId(), reason);

        log.info("Payment journal reversed for receipt: {}", payment.getReceiptNumber());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private void checkNoExistingJournal(PaymentEntity payment) {
        if (payment.getJournalEntryId() != null) {
            boolean journalExists = journalEntryRepository
                    .findBySourceModuleAndSourceEntityIdAndStatus(
                            "PAYMENT", payment.getId(), JournalStatus.POSTED)
                    .isPresent();
            if (journalExists) {
                throw new BusinessException(
                        "Journal already exists for payment: " + payment.getReceiptNumber());
            }
        }
    }

    private void checkPaymentHasJournal(PaymentEntity payment) {
        if (payment.getJournalEntryId() == null) {
            throw new BusinessException(
                    "No journal found for payment. Post the payment journal first.");
        }
    }

    private JournalResponse createAndPostJournal(
            PaymentEntity payment,
            List<CreateJournalLineRequest> lines,
            String description
    ) {
        // Build the journal request
        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(LocalDate.now())
                .reference(payment.getReceiptNumber())
                .description(description)
                .journalType(JournalType.GENERAL)
                .currencyCode(payment.getCurrency() != null ? payment.getCurrency().name() : "GHS")
                .lines(lines)
                .build();

        // Create the journal
        JournalResponse created = journalService.create(request);

        // Update source tracking fields directly on the entity
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("PAYMENT");
        entry.setSourceEntityId(payment.getId());
        journalEntryRepository.save(entry);

        // Post the journal
        JournalResponse posted = journalService.post(created.getId());

        return posted;
    }

    private Long resolveAccountId(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException(
                        "Account not found with ID: " + accountId));
    }
}
