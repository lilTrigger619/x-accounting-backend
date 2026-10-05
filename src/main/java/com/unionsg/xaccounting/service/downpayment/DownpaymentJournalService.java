package com.unionsg.xaccounting.service.downpayment;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.downpayment.Downpayment;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentAllocation;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentRefund;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Posts every GL movement of a downpayment through the existing journal engine
 * ({@link JournalService#create} then {@link JournalService#post}, which also enforces the
 * accounting period lock). Revenue and expense accounts are never touched here: cash received
 * from a customer only ever lands in the downpayment liability, and cash paid to a supplier in
 * the downpayment asset, until it is applied to an invoice/bill.
 *
 * <pre>
 * Customer receipt:      Dr Bank/Cash                      Cr Customer Downpayment Liability
 * Customer application:  Dr Customer Downpayment Liability Cr Accounts Receivable
 * Customer refund:       Dr Customer Downpayment Liability Cr Bank/Cash
 * Supplier payment:      Dr Supplier Downpayment Asset     Cr Bank/Cash
 * Supplier application:  Dr Accounts Payable               Cr Supplier Downpayment Asset
 * Supplier refund:       Dr Bank/Cash                      Cr Supplier Downpayment Asset
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class DownpaymentJournalService {

    public static final String SOURCE_MODULE = "DOWNPAYMENT";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;

    @Transactional
    public JournalEntry postReceipt(Downpayment dp) {
        Long bank = bankAccountId(dp.getBankAccount(), dp.getType());
        Long control = controlAccountId(dp);
        boolean customer = dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT;
        String what = customer ? "Downpayment received from " : "Downpayment paid to ";
        List<CreateJournalLineRequest> lines = customer
                ? lines(bank, control, dp.getAmount(), "Downpayment received " + dp.getDownpaymentNumber())
                : lines(control, bank, dp.getAmount(), "Downpayment paid " + dp.getDownpaymentNumber());
        return createAndPost(dp, dp.getPaymentDate(), lines, "",
                what + dp.getCounterpartyName() + " (" + dp.getDownpaymentNumber() + ").");
    }

    @Transactional
    public JournalEntry postAllocation(Downpayment dp, DownpaymentAllocation allocation) {
        Long control = controlAccountId(dp);
        boolean customer = dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT;
        String doc = allocation.getDocumentNumber();
        List<CreateJournalLineRequest> lines = customer
                ? lines(control, mapped(MappingKey.INVOICE_ACCOUNTS_RECEIVABLE), allocation.getAmount(),
                        "Downpayment " + dp.getDownpaymentNumber() + " applied to " + doc)
                : lines(mapped(MappingKey.BILL_ACCOUNTS_PAYABLE), control, allocation.getAmount(),
                        "Downpayment " + dp.getDownpaymentNumber() + " applied to " + doc);
        return createAndPost(dp, allocation.getAllocationDate(), lines, "-A" + allocation.getId(),
                "Application of downpayment " + dp.getDownpaymentNumber() + " to " + doc + ".");
    }

    @Transactional
    public JournalEntry postRefund(Downpayment dp, DownpaymentRefund refund) {
        Long bank = bankAccountId(refund.getBankAccount(), dp.getType());
        Long control = controlAccountId(dp);
        boolean customer = dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT;
        List<CreateJournalLineRequest> lines = customer
                ? lines(control, bank, refund.getAmount(), "Downpayment refunded " + refund.getRefundNumber())
                : lines(bank, control, refund.getAmount(), "Downpayment refund received " + refund.getRefundNumber());
        return createAndPost(dp, refund.getRefundDate(), lines, "-R" + refund.getId(),
                "Refund " + refund.getRefundNumber() + " of downpayment " + dp.getDownpaymentNumber() + ".");
    }

    /** Reverses a posted journal through the journal engine and tags the reversal to this module. */
    @Transactional
    public JournalEntry reverse(Downpayment dp, JournalEntry original, String reason) {
        if (original == null) {
            throw new BusinessException("Nothing to reverse: no journal was posted for this entry");
        }
        JournalResponse reversed = journalService.reverse(original.getId(), reason);
        JournalEntry entry = journalEntryRepository.findById(reversed.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after reversal"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(dp.getId());
        return journalEntryRepository.save(entry);
    }

    private List<CreateJournalLineRequest> lines(Long debit, Long credit, BigDecimal amount, String description) {
        return List.of(
                CreateJournalLineRequest.builder()
                        .accountId(debit).description(description)
                        .debitAmount(amount).creditAmount(BigDecimal.ZERO).build(),
                CreateJournalLineRequest.builder()
                        .accountId(credit).description(description)
                        .debitAmount(BigDecimal.ZERO).creditAmount(amount).build()
        );
    }

    private JournalEntry createAndPost(Downpayment dp, LocalDate date, List<CreateJournalLineRequest> lines,
                                       String referenceSuffix, String description) {
        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(date)
                .reference(dp.getDownpaymentNumber() + referenceSuffix)
                .description(description)
                .journalType(JournalType.DOWNPAYMENT)
                .currencyCode(dp.getCurrency())
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(dp.getId());
        journalEntryRepository.save(entry);

        JournalResponse posted = journalService.post(created.getId());
        return journalEntryRepository.findById(posted.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after posting"));
    }

    private Long controlAccountId(Downpayment dp) {
        if (dp.getControlAccount() != null) {
            return accountId(dp.getControlAccount().getAccountId());
        }
        return mapped(dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT
                ? MappingKey.CUSTOMER_DOWNPAYMENT_LIABILITY
                : MappingKey.SUPPLIER_DOWNPAYMENT_ASSET);
    }

    private Long bankAccountId(BankAccount bankAccount, DownpaymentType type) {
        if (bankAccount != null && bankAccount.getGlAccountCode() != null && !bankAccount.getGlAccountCode().isBlank()) {
            return accountId(bankAccount.getGlAccountCode());
        }
        return mapped(type == DownpaymentType.CUSTOMER_DOWNPAYMENT
                ? MappingKey.PAYMENT_BANK_ACCOUNT
                : MappingKey.SUPPLIER_PAYMENT_BANK_ACCOUNT);
    }

    private Long accountId(String code) {
        return accountRepository.findByAccountId(code)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with code: " + code));
    }

    private Long mapped(MappingKey key) {
        String code = accountingMappingService.resolve(key);
        return accountRepository.findByAccountId(code)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException(
                        "Required accounting configuration missing: \"" + key.getDescription() + "\" is mapped "
                                + "to account code \"" + code + "\", which does not exist in the Chart of Accounts. "
                                + "Configure it under Settings > Accounting Mappings."));
    }
}
