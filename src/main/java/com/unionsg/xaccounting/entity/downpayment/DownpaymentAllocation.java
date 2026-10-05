package com.unionsg.xaccounting.entity.downpayment;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One application of a downpayment against an invoice (customer side) or a supplier bill
 * (supplier side). Rows are never edited or deleted: undoing one flags it {@code reversed} and
 * posts a reversing journal, so the full history stays visible.
 */
@Entity
@Table(name = "downpayment_allocations", indexes = {
        @Index(name = "idx_dp_alloc_downpayment", columnList = "downpayment_id"),
        @Index(name = "idx_dp_alloc_invoice", columnList = "invoice_id"),
        @Index(name = "idx_dp_alloc_bill", columnList = "bill_id")
})
@Getter
@Setter
public class DownpaymentAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "downpayment_id")
    private Downpayment downpayment;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private SettlementDocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @Column(nullable = false)
    private Boolean reversed = false;

    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_journal_id")
    private JournalEntry reversalJournal;

    public Long getDocumentId() {
        return documentType == SettlementDocumentType.INVOICE
                ? (invoice != null ? invoice.getId() : null)
                : (bill != null ? bill.getId() : null);
    }

    public String getDocumentNumber() {
        return documentType == SettlementDocumentType.INVOICE
                ? (invoice != null ? invoice.getInvoiceNumber() : null)
                : (bill != null ? bill.getBillNumber() : null);
    }
}
