package com.unionsg.xaccounting.entity.downpayment;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Money received from a customer, or paid to a supplier, before the goods or services are
 * delivered. It is never revenue or expense on its own: a customer downpayment sits in a
 * liability account and a supplier downpayment in an asset account until it is applied to an
 * invoice/bill (through {@link DownpaymentAllocation}) or refunded (through
 * {@link DownpaymentRefund}). The original amount is never overwritten; the applied, refunded
 * and available figures are always derived from the allocation and refund history.
 */
@Entity
@Table(name = "downpayments", indexes = {
        @Index(name = "idx_downpayment_customer", columnList = "customer_id"),
        @Index(name = "idx_downpayment_supplier", columnList = "supplier_id"),
        @Index(name = "idx_downpayment_type_status", columnList = "downpayment_type, status")
})
@Getter
@Setter
public class Downpayment extends BaseEntity {

    @Column(name = "downpayment_number", nullable = false, unique = true, length = 100)
    private String downpaymentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "downpayment_type", nullable = false, length = 30)
    private DownpaymentType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Snapshot of the customer/supplier display name for lists and reports. */
    @Column(name = "counterparty_name", nullable = false, length = 200)
    private String counterpartyName;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    /** The bank/cash account the money came into or went out of. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    /**
     * Overrides the mapped Customer Downpayment Liability / Supplier Downpayment Asset
     * account for this record when set.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_account_id")
    private AccountEntity controlAccount;

    @Column(length = 150)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DownpaymentStatus status = DownpaymentStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_journal_id")
    private JournalEntry reversalJournal;

    private LocalDateTime postedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    @OneToMany(mappedBy = "downpayment", cascade = CascadeType.ALL)
    @OrderBy("allocationDate ASC, id ASC")
    private List<DownpaymentAllocation> allocations = new ArrayList<>();

    @OneToMany(mappedBy = "downpayment", cascade = CascadeType.ALL)
    @OrderBy("refundDate ASC, id ASC")
    private List<DownpaymentRefund> refunds = new ArrayList<>();

    public Long getCounterpartyId() {
        if (type == DownpaymentType.CUSTOMER_DOWNPAYMENT) {
            return customer != null ? customer.getId() : null;
        }
        return supplier != null ? supplier.getId() : null;
    }
}
