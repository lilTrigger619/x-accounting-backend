package com.unionsg.xaccounting.entity.prepayment;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentCounterpartyType;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentFrequency;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
 * The Prepayment master record (Prepayments spec §7): a lump sum paid out for goods or services
 * not yet received, recognized into expense over a schedule of future periods instead of all at
 * once. Scoped to the prepaid-expense side only (money the organization pays out in advance) -
 * prepayments received from customers (unearned revenue / customer deposits) are already covered
 * by the existing {@code Payment.unallocatedAmount} / Customer Deposits liability flow from
 * Phase 1, and are out of scope here to avoid two competing models for the same liability.
 */
@Entity
@Table(name = "prepayments")
@Getter
@Setter
public class Prepayment extends BaseEntity {

    @Column(name = "prepayment_number", nullable = false, unique = true, length = 100)
    private String prepaymentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prepayment_type_id")
    private PrepaymentType prepaymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "counterparty_type", nullable = false, length = 30)
    private PrepaymentCounterpartyType counterpartyType;

    @Column(name = "counterparty_name", nullable = false, length = 200)
    private String counterpartyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "recognition_start_date", nullable = false)
    private LocalDate recognitionStartDate;

    @Column(name = "number_of_periods", nullable = false)
    private Integer numberOfPeriods;

    @Enumerated(EnumType.STRING)
    @Column(name = "recognition_frequency", nullable = false, length = 20)
    private PrepaymentFrequency recognitionFrequency;

    @Column(name = "amount_recognized", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountRecognized = BigDecimal.ZERO;

    @Column(name = "amount_remaining", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountRemaining;

    /** Overrides {@code MappingKey.PREPAYMENT_DEFAULT_ASSET} when set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepaid_account_id")
    private AccountEntity prepaidAccount;

    /** Overrides {@code MappingKey.PREPAYMENT_DEFAULT_EXPENSE} when set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_account_id")
    private AccountEntity expenseAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrepaymentStatus status = PrepaymentStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @OneToMany(mappedBy = "prepayment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("periodNumber ASC")
    private List<PrepaymentAmortizationLine> schedule = new ArrayList<>();

    private LocalDateTime activatedAt;

    private LocalDateTime cancelledAt;
}
