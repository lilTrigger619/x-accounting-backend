package com.unionsg.xaccounting.entity.payment;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "supplier_payments",
        indexes = {
                @Index(name = "idx_supplier_payment_number", columnList = "payment_number"),
                @Index(name = "idx_supplier_payment_date", columnList = "payment_date"),
                @Index(name = "idx_supplier_payment_supplier", columnList = "supplier_id"),
                @Index(name = "idx_supplier_payment_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_supplier_payment_number", columnNames = "payment_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPaymentEntity extends BaseEntity {

    @Column(name = "payment_number", nullable = false, length = 100)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /** The specific bank/cash account (Settings & Setup §16) the payment was made from. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_bank_account_id")
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "amount_paid", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "allocated_amount", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "unallocated_amount", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal unallocatedAmount = BigDecimal.ZERO;

    /**
     * Tax withheld from this payment on the supplier's behalf (Supplier.taxInfo's rate),
     * remitted to the tax authority instead of the supplier - the cash actually disbursed
     * is {@code amountPaid} minus this amount, while {@code amountPaid} itself still clears
     * the full allocated/unallocated bill balance.
     */
    @Column(name = "withholding_tax_amount", precision = 19, scale = 2, nullable = false,
            columnDefinition = "numeric(19,2) default 0")
    @Builder.Default
    private BigDecimal withholdingTaxAmount = BigDecimal.ZERO;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "memo", length = 500)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private SupplierPaymentStatus status = SupplierPaymentStatus.DRAFT;

    @Column(name = "fully_allocated", nullable = false)
    @Builder.Default
    private Boolean fullyAllocated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @ElementCollection
    @CollectionTable(
            name = "supplier_payment_attachments",
            joinColumns = @JoinColumn(name = "supplier_payment_id")
    )
    @Column(name = "attachment_id")
    @Builder.Default
    private List<Long> attachments = new ArrayList<>();

    @OneToMany(
            mappedBy = "supplierPayment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<SupplierPaymentAllocationEntity> allocations = new ArrayList<>();

    // ===== Convenience Methods =====

    public void addAllocation(SupplierPaymentAllocationEntity allocation) {
        allocation.setSupplierPayment(this);
        this.allocations.add(allocation);
    }

    public void removeAllocation(SupplierPaymentAllocationEntity allocation) {
        allocation.setSupplierPayment(null);
        this.allocations.remove(allocation);
    }
}
