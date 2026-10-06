package com.unionsg.xaccounting.entity.settlement;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;
import com.unionsg.xaccounting.enums.settlement.SettlementSourceType;
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
 * One amount of a non-cash source (a deposit or a downpayment) applied against an invoice or a
 * supplier bill. This is the shared ledger every such module writes through
 * {@code DocumentSettlementService}, so an invoice or bill can show exactly how much of its
 * total was settled by each source without each module keeping its own competing balance logic.
 * Reversing a settlement keeps the row (flagged {@code reversed}) for the audit trail.
 */
@Entity
@Table(name = "document_settlements", indexes = {
        @Index(name = "idx_doc_settlement_invoice", columnList = "invoice_id"),
        @Index(name = "idx_doc_settlement_bill", columnList = "bill_id"),
        @Index(name = "idx_doc_settlement_source", columnList = "source_type, source_allocation_id")
})
@Getter
@Setter
public class DocumentSettlement extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20)
    private SettlementDocumentType documentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id")
    private Bill bill;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SettlementSourceType sourceType;

    /** The deposit / downpayment record the amount came from. */
    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    /** The source module's own allocation row, so one allocation can only ever settle once. */
    @Column(name = "source_allocation_id", nullable = false)
    private Long sourceAllocationId;

    @Column(name = "source_number", length = 100)
    private String sourceNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "settlement_date", nullable = false)
    private LocalDate settlementDate;

    @Column(nullable = false)
    private Boolean reversed = false;

    private LocalDateTime reversedAt;
}
