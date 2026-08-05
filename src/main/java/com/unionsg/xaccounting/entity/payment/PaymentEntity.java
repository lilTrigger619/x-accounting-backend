package com.unionsg.xaccounting.entity.payment;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_receipt_number", columnList = "receipt_number"),
                @Index(name = "idx_payment_date", columnList = "payment_date"),
                @Index(name = "idx_payment_customer", columnList = "customer_id"),
                @Index(name = "idx_payment_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_receipt_number", columnNames = "receipt_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity extends BaseEntity {

    @Column(name = "receipt_number", nullable = false, length = 100)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private ChartOfAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    @Builder.Default
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "amount_received", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal amountReceived = BigDecimal.ZERO;

    @Column(name = "allocated_amount", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal allocatedAmount = BigDecimal.ZERO;

    @Column(name = "unallocated_amount", precision = 19, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal unallocatedAmount = BigDecimal.ZERO;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "memo", length = 500)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.DRAFT;

    @Column(name = "fully_allocated", nullable = false)
    @Builder.Default
    private Boolean fullyAllocated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @ElementCollection
    @CollectionTable(
            name = "payment_attachments",
            joinColumns = @JoinColumn(name = "payment_id")
    )
    @Column(name = "attachment_id")
    @Builder.Default
    private List<Long> attachments = new ArrayList<>();

    @OneToMany(
            mappedBy = "payment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PaymentAllocationEntity> allocations = new ArrayList<>();

    @OneToMany(
            mappedBy = "payment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PaymentRefundEntity> refunds = new ArrayList<>();

    // ===== Convenience Methods =====

    public void addAllocation(PaymentAllocationEntity allocation) {
        allocation.setPayment(this);
        this.allocations.add(allocation);
    }

    public void removeAllocation(PaymentAllocationEntity allocation) {
        allocation.setPayment(null);
        this.allocations.remove(allocation);
    }

    public void addRefund(PaymentRefundEntity refund) {
        refund.setPayment(this);
        this.refunds.add(refund);
    }

    public void removeRefund(PaymentRefundEntity refund) {
        refund.setPayment(null);
        this.refunds.remove(refund);
    }
}
