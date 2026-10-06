package com.unionsg.xaccounting.service.settlement;

import com.unionsg.xaccounting.dto.settlement.DocumentSettlementResponse;
import com.unionsg.xaccounting.dto.settlement.SettlementSummaryResponse;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.settlement.DocumentSettlement;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;
import com.unionsg.xaccounting.enums.settlement.SettlementSourceType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.settlement.DocumentSettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The one place non-cash sources (deposits, downpayments, and later credit notes) settle an
 * invoice or a supplier bill. It validates the target document, moves the document's
 * {@code amountPaid}/{@code balance}/status exactly the way payment allocation does, and records
 * a {@link DocumentSettlement} row so the document can show what settled it.
 *
 * <p>The calling module owns its own allocation row and the GL journal; it passes that
 * allocation's id as {@code sourceAllocationId}, which this service uses to refuse a duplicate
 * settlement and to find the row again on reversal.</p>
 */
@Service
@RequiredArgsConstructor
public class DocumentSettlementService {

    private final DocumentSettlementRepository repository;
    private final InvoiceRepository invoiceRepository;
    private final BillRepository billRepository;

    /** Checks an application before the caller posts its journal; throws on any problem. */
    @Transactional(readOnly = true)
    public Invoice validateInvoiceSettlement(Long invoiceId, Long customerId, String currency, BigDecimal amount) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with ID: " + invoiceId));
        if (invoice.getCustomer() == null || customerId == null || !invoice.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("Invoice " + invoice.getInvoiceNumber() + " belongs to a different customer");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Invoice " + invoice.getInvoiceNumber() + " is cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new BusinessException("Invoice " + invoice.getInvoiceNumber() + " is still a draft and has not been posted to receivables");
        }
        assertCurrency(currency, invoice.getCurrency(), "Invoice " + invoice.getInvoiceNumber());
        assertAmount(amount, invoice.getBalance(), "invoice " + invoice.getInvoiceNumber());
        return invoice;
    }

    @Transactional(readOnly = true)
    public Bill validateBillSettlement(Long billId, Long supplierId, String currency, BigDecimal amount) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("Bill not found with ID: " + billId));
        if (bill.getSupplier() == null || supplierId == null || !bill.getSupplier().getId().equals(supplierId)) {
            throw new BusinessException("Bill " + bill.getBillNumber() + " belongs to a different supplier");
        }
        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new BusinessException("Bill " + bill.getBillNumber() + " is cancelled");
        }
        if (bill.getStatus() == BillStatus.DRAFT) {
            throw new BusinessException("Bill " + bill.getBillNumber() + " is still a draft and has not been posted to payables");
        }
        assertCurrency(currency, bill.getCurrency(), "Bill " + bill.getBillNumber());
        assertAmount(amount, bill.getBalance(), "bill " + bill.getBillNumber());
        return bill;
    }

    @Transactional
    public DocumentSettlement settleInvoice(
            Long invoiceId, Long customerId, String currency, BigDecimal amount, LocalDate date,
            SettlementSourceType sourceType, Long sourceId, Long sourceAllocationId, String sourceNumber
    ) {
        assertNotDuplicate(sourceType, sourceAllocationId);
        Invoice invoice = validateInvoiceSettlement(invoiceId, customerId, currency, amount);

        invoice.setAmountPaid(nz(invoice.getAmountPaid()).add(amount));
        invoice.setBalance(nz(invoice.getBalance()).subtract(amount));
        updateInvoiceStatus(invoice);
        invoiceRepository.save(invoice);

        DocumentSettlement settlement = newSettlement(SettlementDocumentType.INVOICE, amount, date,
                sourceType, sourceId, sourceAllocationId, sourceNumber);
        settlement.setInvoice(invoice);
        return repository.save(settlement);
    }

    @Transactional
    public DocumentSettlement settleBill(
            Long billId, Long supplierId, String currency, BigDecimal amount, LocalDate date,
            SettlementSourceType sourceType, Long sourceId, Long sourceAllocationId, String sourceNumber
    ) {
        assertNotDuplicate(sourceType, sourceAllocationId);
        Bill bill = validateBillSettlement(billId, supplierId, currency, amount);

        bill.setAmountPaid(nz(bill.getAmountPaid()).add(amount));
        bill.setBalance(nz(bill.getBalance()).subtract(amount));
        updateBillStatus(bill);
        billRepository.save(bill);

        DocumentSettlement settlement = newSettlement(SettlementDocumentType.BILL, amount, date,
                sourceType, sourceId, sourceAllocationId, sourceNumber);
        settlement.setBill(bill);
        return repository.save(settlement);
    }

    /** Puts the amount back on the invoice/bill and flags the settlement row as reversed. */
    @Transactional
    public void reverse(SettlementSourceType sourceType, Long sourceAllocationId) {
        DocumentSettlement settlement = repository
                .findBySourceTypeAndSourceAllocationIdAndReversedFalse(sourceType, sourceAllocationId)
                .orElseThrow(() -> new BusinessException("No active settlement found for this allocation"));

        BigDecimal amount = settlement.getAmount();
        if (settlement.getDocumentType() == SettlementDocumentType.INVOICE) {
            Invoice invoice = settlement.getInvoice();
            invoice.setAmountPaid(nz(invoice.getAmountPaid()).subtract(amount));
            invoice.setBalance(nz(invoice.getBalance()).add(amount));
            updateInvoiceStatus(invoice);
            invoiceRepository.save(invoice);
        } else {
            Bill bill = settlement.getBill();
            bill.setAmountPaid(nz(bill.getAmountPaid()).subtract(amount));
            bill.setBalance(nz(bill.getBalance()).add(amount));
            updateBillStatus(bill);
            billRepository.save(bill);
        }

        settlement.setReversed(true);
        settlement.setReversedAt(LocalDateTime.now());
        repository.save(settlement);
    }

    @Transactional(readOnly = true)
    public SettlementSummaryResponse invoiceSummary(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with ID: " + invoiceId));
        List<DocumentSettlement> rows = repository.findByInvoiceIdOrderBySettlementDateAscIdAsc(invoiceId);
        return summarize(SettlementDocumentType.INVOICE, invoice.getId(), invoice.getInvoiceNumber(),
                invoice.getCurrency(), invoice.getTotalAmount(), invoice.getAmountPaid(), invoice.getBalance(), rows);
    }

    @Transactional(readOnly = true)
    public SettlementSummaryResponse billSummary(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("Bill not found with ID: " + billId));
        List<DocumentSettlement> rows = repository.findByBillIdOrderBySettlementDateAscIdAsc(billId);
        return summarize(SettlementDocumentType.BILL, bill.getId(), bill.getBillNumber(),
                bill.getCurrency(), bill.getTotalAmount(), bill.getAmountPaid(), bill.getBalance(), rows);
    }

    private SettlementSummaryResponse summarize(
            SettlementDocumentType type, Long id, String number, String currency,
            BigDecimal total, BigDecimal amountPaid, BigDecimal balance, List<DocumentSettlement> rows
    ) {
        SettlementSummaryResponse summary = new SettlementSummaryResponse();
        summary.setDocumentType(type);
        summary.setDocumentId(id);
        summary.setDocumentNumber(number);
        summary.setCurrency(currency);
        summary.setOriginalAmount(nz(total));

        BigDecimal nonCash = BigDecimal.ZERO;
        for (SettlementSourceType source : SettlementSourceType.values()) {
            summary.getAppliedBySource().put(source.name(), BigDecimal.ZERO);
        }
        for (DocumentSettlement row : rows) {
            if (!Boolean.TRUE.equals(row.getReversed())) {
                summary.getAppliedBySource().merge(row.getSourceType().name(), row.getAmount(), BigDecimal::add);
                nonCash = nonCash.add(row.getAmount());
            }
            summary.getSettlements().add(toResponse(row));
        }

        summary.setPaymentsApplied(nz(amountPaid).subtract(nonCash).max(BigDecimal.ZERO));
        summary.setTotalSettled(nz(amountPaid));
        summary.setAmountDue(nz(balance));
        return summary;
    }

    public static DocumentSettlementResponse toResponse(DocumentSettlement row) {
        DocumentSettlementResponse response = new DocumentSettlementResponse();
        response.setId(row.getId());
        response.setDocumentType(row.getDocumentType());
        if (row.getDocumentType() == SettlementDocumentType.INVOICE && row.getInvoice() != null) {
            response.setDocumentId(row.getInvoice().getId());
            response.setDocumentNumber(row.getInvoice().getInvoiceNumber());
        } else if (row.getBill() != null) {
            response.setDocumentId(row.getBill().getId());
            response.setDocumentNumber(row.getBill().getBillNumber());
        }
        response.setSourceType(row.getSourceType());
        response.setSourceTypeLabel(row.getSourceType().getLabel());
        response.setSourceId(row.getSourceId());
        response.setSourceAllocationId(row.getSourceAllocationId());
        response.setSourceNumber(row.getSourceNumber());
        response.setAmount(row.getAmount());
        response.setSettlementDate(row.getSettlementDate());
        response.setReversed(row.getReversed());
        response.setReversedAt(row.getReversedAt());
        return response;
    }

    private DocumentSettlement newSettlement(
            SettlementDocumentType type, BigDecimal amount, LocalDate date,
            SettlementSourceType sourceType, Long sourceId, Long sourceAllocationId, String sourceNumber
    ) {
        DocumentSettlement settlement = new DocumentSettlement();
        settlement.setDocumentType(type);
        settlement.setAmount(amount);
        settlement.setSettlementDate(date != null ? date : LocalDate.now());
        settlement.setSourceType(sourceType);
        settlement.setSourceId(sourceId);
        settlement.setSourceAllocationId(sourceAllocationId);
        settlement.setSourceNumber(sourceNumber);
        settlement.setReversed(false);
        return settlement;
    }

    private void assertNotDuplicate(SettlementSourceType sourceType, Long sourceAllocationId) {
        if (sourceAllocationId == null) {
            throw new BusinessException("A settlement must reference the allocation it came from");
        }
        if (repository.existsBySourceTypeAndSourceAllocationIdAndReversedFalse(sourceType, sourceAllocationId)) {
            throw new BusinessException("This allocation has already been applied");
        }
    }

    private void assertAmount(BigDecimal amount, BigDecimal balance, String label) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("The amount applied must be positive");
        }
        if (amount.compareTo(nz(balance)) > 0) {
            throw new BusinessException("Amount " + amount + " exceeds the outstanding balance of "
                    + nz(balance) + " on " + label);
        }
    }

    private void assertCurrency(String sourceCurrency, String documentCurrency, String label) {
        if (sourceCurrency != null && documentCurrency != null && !sourceCurrency.equalsIgnoreCase(documentCurrency)) {
            throw new BusinessException(label + " is in " + documentCurrency + ", not " + sourceCurrency);
        }
    }

    private void updateInvoiceStatus(Invoice invoice) {
        BigDecimal balance = nz(invoice.getBalance());
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
        } else if (balance.compareTo(nz(invoice.getTotalAmount())) < 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {
            invoice.setStatus(InvoiceStatus.SENT);
        }
    }

    private void updateBillStatus(Bill bill) {
        BigDecimal balance = nz(bill.getBalance());
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            bill.setStatus(BillStatus.PAID);
            bill.setPaidAt(LocalDateTime.now());
        } else if (balance.compareTo(nz(bill.getTotalAmount())) < 0) {
            bill.setStatus(BillStatus.PARTIALLY_PAID);
        } else {
            bill.setStatus(BillStatus.OPEN);
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
