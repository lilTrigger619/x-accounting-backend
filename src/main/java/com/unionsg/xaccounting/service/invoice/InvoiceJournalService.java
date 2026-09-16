package com.unionsg.xaccounting.service.invoice;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Posts the GL impact of a sales invoice once it is sent (Dr Accounts Receivable, Cr Revenue,
 * Cr Sales Tax Payable for the tax portion). Mirrors {@code APJournalService}'s pattern on the
 * payable side. Without this, Accounts Receivable is only ever credited as payments come in
 * (see {@code PaymentJournalServiceImpl}) and never debited for the sales that created the
 * receivable in the first place, so the AR control account balance would only ever go negative.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceJournalService {

    public static final String SOURCE_MODULE = "INVOICE";

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final AccountingMappingService accountingMappingService;

    @Transactional
    public void postInvoiceJournal(Invoice invoice) {
        checkNoExistingInvoiceJournal(invoice);

        Long arAccountIdResolved = resolveMappedAccountId(MappingKey.INVOICE_ACCOUNTS_RECEIVABLE);
        Long revenueAccountIdResolved = resolveMappedAccountId(MappingKey.INVOICE_REVENUE);
        Long salesTaxPayableAccountIdResolved = resolveMappedAccountId(MappingKey.INVOICE_SALES_TAX_PAYABLE);

        BigDecimal netRevenue = invoice.getSubtotal().subtract(invoice.getDiscountAmount());
        BigDecimal salesTax = invoice.getTotalTax();

        List<CreateJournalLineRequest> lines = new ArrayList<>();

        lines.add(CreateJournalLineRequest.builder()
                .accountId(arAccountIdResolved)
                .description("Invoice " + invoice.getInvoiceNumber() + " billed to "
                        + (invoice.getCustomer() != null ? invoice.getCustomer().getDisplayName() : "Unknown"))
                .debitAmount(invoice.getTotalAmount())
                .creditAmount(BigDecimal.ZERO)
                .build());

        lines.add(CreateJournalLineRequest.builder()
                .accountId(revenueAccountIdResolved)
                .description("Revenue for invoice " + invoice.getInvoiceNumber())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(netRevenue)
                .build());

        if (salesTax.compareTo(BigDecimal.ZERO) > 0) {
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(salesTaxPayableAccountIdResolved)
                    .description("Sales tax collected on invoice " + invoice.getInvoiceNumber())
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(salesTax)
                    .build());
        }

        String description = "Invoice " + invoice.getInvoiceNumber() + " sent to "
                + (invoice.getCustomer() != null ? invoice.getCustomer().getDisplayName() : "Unknown") + ".";

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(invoice.getIssueDate())
                .reference(invoice.getInvoiceNumber())
                .description(description)
                .journalType(JournalType.SALES)
                .currencyCode(invoice.getCurrency() != null ? invoice.getCurrency() : "USD")
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);

        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule(SOURCE_MODULE);
        entry.setSourceEntityId(invoice.getId());
        journalEntryRepository.save(entry);

        journalService.post(created.getId());

        log.info("Invoice journal posted for: {}", invoice.getInvoiceNumber());
    }

    private void checkNoExistingInvoiceJournal(Invoice invoice) {
        boolean exists = journalEntryRepository
                .findBySourceModuleAndSourceEntityIdAndStatus(SOURCE_MODULE, invoice.getId(), JournalStatus.POSTED)
                .isPresent();
        if (exists) {
            throw new BusinessException("Journal already exists for invoice: " + invoice.getInvoiceNumber());
        }
    }

    private Long resolveAccountId(String accountId) {
        return accountRepository.findByAccountId(accountId)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException(
                        "Account not found with ID: " + accountId));
    }

    /**
     * Resolves a centrally-configured mapping (Settings & Setup §8) to a postable account ID,
     * failing with a message that tells the admin exactly what to fix rather than a bare
     * "Account not found" (§38's "required configuration validation" applied to the one place
     * in the app most likely to be posted before Settings has been touched: a new invoice).
     */
    private Long resolveMappedAccountId(MappingKey key) {
        String code = accountingMappingService.resolve(key);
        return accountRepository.findByAccountId(code)
                .map(account -> Long.valueOf(account.getAccountId()))
                .orElseThrow(() -> new BusinessException(
                        "Required accounting configuration missing: \"" + key.getDescription() + "\" is mapped "
                                + "to account code \"" + code + "\", which does not exist in the Chart of Accounts. "
                                + "Configure it under Settings > Accounting Mappings before posting invoices."));
    }
}
