package com.unionsg.xaccounting.service.invoice;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.invoice.InvoiceItem;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        String defaultRevenueCode = accountingMappingService.resolve(MappingKey.INVOICE_REVENUE);
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

        for (Map.Entry<String, BigDecimal> revenueLine : splitRevenueByAccount(invoice, netRevenue, defaultRevenueCode).entrySet()) {
            if (revenueLine.getValue().compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            lines.add(CreateJournalLineRequest.builder()
                    .accountId(resolveAccountId(revenueLine.getKey()))
                    .description("Revenue for invoice " + invoice.getInvoiceNumber())
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(revenueLine.getValue())
                    .build());
        }

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

    /**
     * Splits an invoice's net revenue across the GL account each line resolves to: its
     * {@code Product}'s income account when one is set, otherwise the centrally-configured
     * default (§8) - previously every invoice posted its whole net revenue to that one default
     * account regardless of which products were sold, ignoring Product.incomeAccount entirely.
     * The invoice-level discount is prorated across accounts by each one's share of the raw
     * (pre-discount) subtotal; the last account absorbs any rounding remainder so the lines
     * always sum to exactly {@code netRevenue}.
     */
    private Map<String, BigDecimal> splitRevenueByAccount(Invoice invoice, BigDecimal netRevenue, String defaultRevenueCode) {
        BigDecimal rawSubtotal = invoice.getSubtotal() != null ? invoice.getSubtotal() : BigDecimal.ZERO;
        if (rawSubtotal.compareTo(BigDecimal.ZERO) == 0) {
            Map<String, BigDecimal> single = new LinkedHashMap<>();
            single.put(defaultRevenueCode, netRevenue);
            return single;
        }

        Map<String, BigDecimal> subtotalByAccount = new LinkedHashMap<>();
        for (InvoiceItem item : invoice.getItems()) {
            String accountCode = item.getProduct() != null && item.getProduct().getIncomeAccount() != null
                    ? item.getProduct().getIncomeAccount().getAccountId()
                    : defaultRevenueCode;
            BigDecimal lineSubtotal = item.getLineSubtotal() != null ? item.getLineSubtotal() : BigDecimal.ZERO;
            subtotalByAccount.merge(accountCode, lineSubtotal, BigDecimal::add);
        }
        if (subtotalByAccount.isEmpty()) {
            subtotalByAccount.put(defaultRevenueCode, rawSubtotal);
        }

        Map<String, BigDecimal> netByAccount = new LinkedHashMap<>();
        BigDecimal remainingNet = netRevenue;
        int index = 0;
        int groupCount = subtotalByAccount.size();
        for (Map.Entry<String, BigDecimal> entry : subtotalByAccount.entrySet()) {
            index++;
            BigDecimal groupNet;
            if (index == groupCount) {
                groupNet = remainingNet;
            } else {
                BigDecimal share = entry.getValue().divide(rawSubtotal, 10, RoundingMode.HALF_UP);
                groupNet = netRevenue.multiply(share).setScale(2, RoundingMode.HALF_UP);
                remainingNet = remainingNet.subtract(groupNet);
            }
            netByAccount.put(entry.getKey(), groupNet);
        }
        return netByAccount;
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
