package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentEntity;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.repository.payment.SupplierPaymentRepository;
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

/**
 * Posts the GL impact for Accounts Payable transactions (bills and supplier payments).
 * Mirrors the debit/credit pattern used by {@code PaymentJournalServiceImpl} on the
 * Accounts Receivable side, applied in the opposite direction for payables.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class APJournalService {

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final BillRepository billRepository;
    private final SupplierPaymentRepository supplierPaymentRepository;

    @Value("${bill.journal.accounts-payable-account-id}")
    private String accountsPayableAccountId;

    @Value("${bill.journal.default-expense-account-id}")
    private String defaultExpenseAccountId;

    @Value("${supplierpayment.journal.bank-account-id}")
    private String bankAccountId;

    @Value("${supplierpayment.journal.supplier-advances-account-id}")
    private String supplierAdvancesAccountId;

    // ========================================================================
    // BILL RECORDED  (Dr Expense, Cr Accounts Payable)
    // ========================================================================

    @Transactional
    public void postBillJournal(Bill bill) {
        checkNoExistingBillJournal(bill);

        Long expenseAccountIdResolved = resolveAccountId(defaultExpenseAccountId);
        Long apAccountIdResolved = resolveAccountId(accountsPayableAccountId);

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        lines.add(CreateJournalLineRequest.builder()
                .accountId(expenseAccountIdResolved)
                .description("Bill recorded: " + bill.getBillNumber())
                .debitAmount(bill.getTotalAmount())
                .creditAmount(BigDecimal.ZERO)
                .build());

        lines.add(CreateJournalLineRequest.builder()
                .accountId(apAccountIdResolved)
                .description("Accounts payable for " + bill.getBillNumber())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(bill.getTotalAmount())
                .build());

        String description = "Bill " + bill.getBillNumber() + " recorded from "
                + (bill.getSupplier() != null ? bill.getSupplier().getDisplayName() : "Unknown")
                + ".";

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(LocalDate.now())
                .reference(bill.getBillNumber())
                .description(description)
                .journalType(JournalType.PURCHASE)
                .currencyCode(bill.getCurrency() != null ? bill.getCurrency() : "USD")
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);

        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("BILL");
        entry.setSourceEntityId(bill.getId());
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());

        bill.setJournal(journalEntryRepository.findById(posted.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after posting")));
        billRepository.save(bill);

        log.info("Bill journal posted for: {}", bill.getBillNumber());
    }

    // ========================================================================
    // SUPPLIER PAYMENT MADE  (Dr Accounts Payable / Dr Supplier Advances, Cr Bank)
    // ========================================================================

    @Transactional
    public void postSupplierPaymentJournal(SupplierPaymentEntity payment) {
        checkNoExistingPaymentJournal(payment);

        Long bankAccountIdResolved = resolveAccountId(bankAccountId);
        Long apAccountIdResolved = resolveAccountId(accountsPayableAccountId);
        Long advancesAccountIdResolved = resolveAccountId(supplierAdvancesAccountId);

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        if (payment.getAllocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(apAccountIdResolved)
                    .description("Allocated amount for " + payment.getPaymentNumber())
                    .debitAmount(payment.getAllocatedAmount())
                    .creditAmount(BigDecimal.ZERO)
                    .build());
        }

        if (payment.getUnallocatedAmount().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(advancesAccountIdResolved)
                    .description("Unallocated (advance) balance for " + payment.getPaymentNumber())
                    .debitAmount(payment.getUnallocatedAmount())
                    .creditAmount(BigDecimal.ZERO)
                    .build());
        }

        lines.add(CreateJournalLineRequest.builder()
                .accountId(bankAccountIdResolved)
                .description("Payment made: " + payment.getPaymentNumber())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(payment.getAmountPaid())
                .build());

        String description = "Payment " + payment.getPaymentNumber() + " made to "
                + (payment.getSupplier() != null ? payment.getSupplier().getDisplayName() : "Unknown")
                + ".";

        JournalResponse posted = createAndPostSupplierPaymentJournal(payment, lines, description);

        payment.setJournal(journalEntryRepository.findById(posted.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after posting")));
        supplierPaymentRepository.save(payment);

        log.info("Supplier payment journal posted for: {}", payment.getPaymentNumber());
    }

    // ========================================================================
    // ADDITIONAL / REMOVED ALLOCATION ADJUSTMENTS
    // ========================================================================

    @Transactional
    public void postAdditionalAllocationJournal(SupplierPaymentEntity payment, SupplierPaymentAllocationEntity allocation) {
        checkPaymentHasJournal(payment);

        Long apAccountIdResolved = resolveAccountId(accountsPayableAccountId);
        Long advancesAccountIdResolved = resolveAccountId(supplierAdvancesAccountId);

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        lines.add(CreateJournalLineRequest.builder()
                .accountId(apAccountIdResolved)
                .description("Allocation of unallocated balance")
                .debitAmount(allocation.getAllocatedAmount())
                .creditAmount(BigDecimal.ZERO)
                .build());

        lines.add(CreateJournalLineRequest.builder()
                .accountId(advancesAccountIdResolved)
                .description("Additional allocation for bill")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(allocation.getAllocatedAmount())
                .build());

        String description = "Additional allocation of " + allocation.getAllocatedAmount()
                + " for " + payment.getPaymentNumber() + ".";

        createAndPostSupplierPaymentJournal(payment, lines, description);

        log.info("Additional allocation journal posted for supplier payment: {}", payment.getPaymentNumber());
    }

    @Transactional
    public void postRemoveAllocationJournal(SupplierPaymentEntity payment, SupplierPaymentAllocationEntity allocation) {
        checkPaymentHasJournal(payment);

        Long apAccountIdResolved = resolveAccountId(accountsPayableAccountId);
        Long advancesAccountIdResolved = resolveAccountId(supplierAdvancesAccountId);

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        lines.add(CreateJournalLineRequest.builder()
                .accountId(advancesAccountIdResolved)
                .description("Reversal of allocation")
                .debitAmount(allocation.getAllocatedAmount())
                .creditAmount(BigDecimal.ZERO)
                .build());

        lines.add(CreateJournalLineRequest.builder()
                .accountId(apAccountIdResolved)
                .description("Reversal of allocation")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(allocation.getAllocatedAmount())
                .build());

        String description = "Removal of allocation of " + allocation.getAllocatedAmount()
                + " for " + payment.getPaymentNumber() + ".";

        createAndPostSupplierPaymentJournal(payment, lines, description);

        log.info("Allocation removal journal posted for supplier payment: {}", payment.getPaymentNumber());
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private void checkNoExistingBillJournal(Bill bill) {
        if (bill.getJournal() != null) {
            boolean exists = journalEntryRepository
                    .findBySourceModuleAndSourceEntityIdAndStatus("BILL", bill.getId(), JournalStatus.POSTED)
                    .isPresent();
            if (exists) {
                throw new BusinessException("Journal already exists for bill: " + bill.getBillNumber());
            }
        }
    }

    private void checkNoExistingPaymentJournal(SupplierPaymentEntity payment) {
        if (payment.getJournal() != null) {
            boolean exists = journalEntryRepository
                    .findBySourceModuleAndSourceEntityIdAndStatus("SUPPLIER_PAYMENT", payment.getId(), JournalStatus.POSTED)
                    .isPresent();
            if (exists) {
                throw new BusinessException("Journal already exists for payment: " + payment.getPaymentNumber());
            }
        }
    }

    private void checkPaymentHasJournal(SupplierPaymentEntity payment) {
        if (payment.getJournal() == null) {
            throw new BusinessException("No journal found for supplier payment. Post the payment journal first.");
        }
    }

    private JournalResponse createAndPostSupplierPaymentJournal(
            SupplierPaymentEntity payment,
            List<CreateJournalLineRequest> lines,
            String description
    ) {
        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(LocalDate.now())
                .reference(payment.getPaymentNumber())
                .description(description)
                .journalType(JournalType.PURCHASE)
                .currencyCode(payment.getCurrency() != null ? payment.getCurrency().name() : "USD")
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);

        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("SUPPLIER_PAYMENT");
        entry.setSourceEntityId(payment.getId());
        journalEntryRepository.save(entry);

        return journalService.post(created.getId());
    }

    private Long resolveAccountId(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException(
                        "Account not found with ID: " + accountId));
    }
}
