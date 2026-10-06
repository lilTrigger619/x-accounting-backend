package com.unionsg.xaccounting.entity.deposit;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.deposit.DepositCounterpartyType;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import com.unionsg.xaccounting.enums.deposit.DepositStatus;
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
 * A deposit paid by the organization (held as an asset until it comes back, is applied to a
 * supplier bill, or is forfeited) or received from a customer/third party (held as a liability
 * until it is refunded, applied to an invoice, or forfeited). Receiving or paying a deposit
 * never touches income or expense; only a forfeiture does.
 *
 * <p>{@code availableBalance = amount - applied - refunded - forfeited - transferred}, kept in
 * step by {@code DepositService} from the non-reversed rows in {@link #allocations}.</p>
 */
@Entity
@Table(name = "deposits")
@Getter
@Setter
public class Deposit extends BaseEntity {

    @Column(name = "deposit_number", nullable = false, unique = true, length = 100)
    private String depositNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DepositDirection direction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "deposit_type_id")
    private DepositType depositType;

    @Enumerated(EnumType.STRING)
    @Column(name = "counterparty_type", nullable = false, length = 30)
    private DepositCounterpartyType counterpartyType;

    @Column(name = "counterparty_name", nullable = false, length = 200)
    private String counterpartyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(nullable = false)
    private Boolean refundable = true;

    @Column(length = 500)
    private String purpose;

    @Column(length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    /** Overrides the deposit type's account and the direction's mapped default when set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deposit_account_id")
    private AccountEntity depositAccount;

    @Column(nullable = false)
    private Boolean interestBearing = false;

    /** Annual simple-interest rate, in percent. */
    @Column(name = "interest_rate", precision = 9, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "interest_start_date")
    private LocalDate interestStartDate;

    @Column(name = "interest_terms", length = 500)
    private String interestTerms;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DepositStatus status = DepositStatus.DRAFT;

    @Column(name = "applied_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal appliedAmount = BigDecimal.ZERO;

    @Column(name = "refunded_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "forfeited_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal forfeitedAmount = BigDecimal.ZERO;

    @Column(name = "transferred_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal transferredAmount = BigDecimal.ZERO;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    /** The activation journal (Dr asset / Cr bank, or Dr bank / Cr liability). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    /** Set when this deposit was created by transferring part of another one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transferred_from_id")
    private Deposit transferredFrom;

    @OneToMany(mappedBy = "deposit", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("allocationDate ASC, id ASC")
    private List<DepositAllocation> allocations = new ArrayList<>();

    private LocalDateTime activatedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;
}
