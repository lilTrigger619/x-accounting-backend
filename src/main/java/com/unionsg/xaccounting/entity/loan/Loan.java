package com.unionsg.xaccounting.entity.loan;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.loan.LoanCounterpartyType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
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
 * The Loan master record: covers both a loan the organization borrows (direction
 * {@code BORROWED_LOAN} - from a bank, financial institution, shareholder, director or other
 * lender) and a loan the organization lends out (direction {@code LENT_LOAN} - to an employee,
 * customer, supplier or other party). Which side of the balance sheet a disbursement or payment
 * hits is driven entirely by {@code direction}.
 *
 * <p>Columns added after the first release are nullable in the database (ddl-auto adds them to
 * tables that already hold rows) and default in Java; manual migration 009 backfills them.</p>
 */
@Entity
@Table(name = "loans")
@Getter
@Setter
public class Loan extends BaseEntity {

    @Column(name = "loan_number", nullable = false, unique = true, length = 100)
    private String loanNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_type_id")
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "counterparty_type", nullable = false, length = 30)
    private LoanCounterpartyType counterpartyType;

    @Column(name = "counterparty_name", nullable = false, length = 200)
    private String counterpartyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    /** A code from the "currencies" configuration. */
    @Column(length = 10)
    private String currency;

    @Column(name = "interest_rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_method", nullable = false, length = 30)
    private LoanInterestMethod interestMethod = LoanInterestMethod.FIXED_INSTALLMENT;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_frequency", nullable = false, length = 20)
    private LoanFrequency paymentFrequency;

    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments;

    /** Leading installments that carry interest (and fees) only; principal is repaid over the rest. */
    @Column(name = "grace_period_installments")
    private Integer gracePeriodInstallments = 0;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingPrincipal = BigDecimal.ZERO;

    @Column(name = "outstanding_interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    /** Interest accrued to the GL ahead of payment and not yet settled by a payment. */
    @Column(name = "accrued_interest_total", precision = 19, scale = 2)
    private BigDecimal accruedInterestTotal = BigDecimal.ZERO;

    /** Upfront fees charged when the loan is disbursed. */
    @Column(name = "total_fees", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFees = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_treatment", length = 30)
    private LoanFeeTreatment feeTreatment;

    /** A fee charged with every installment (e.g. a monthly service fee). */
    @Column(name = "installment_fee", precision = 19, scale = 2)
    private BigDecimal installmentFee = BigDecimal.ZERO;

    /** When true, a payment above the outstanding balance is accepted and held as an overpayment. */
    @Column(name = "allow_overpayment")
    private Boolean allowOverpayment = false;

    /** Paid in excess of the whole outstanding balance. Sits in the principal account. */
    @Column(name = "overpayment_balance", precision = 19, scale = 2)
    private BigDecimal overpaymentBalance = BigDecimal.ZERO;

    @Column(name = "written_off_amount", precision = 19, scale = 2)
    private BigDecimal writtenOffAmount = BigDecimal.ZERO;

    @Column(name = "collateral_description", columnDefinition = "TEXT")
    private String collateralDescription;

    @Column(name = "collateral_value", precision = 19, scale = 2)
    private BigDecimal collateralValue;

    /** The lender's or borrower's own reference, an agreement number, etc. */
    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.DRAFT;

    /** Set when a DEFAULTED loan was closed with its remaining balance written off. */
    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    /** Overrides {@code MappingKey.LOAN_RECEIVABLE}/{@code LOAN_PAYABLE} when set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "principal_account_id")
    private AccountEntity principalAccount;

    /** Overrides {@code MappingKey.LOAN_INTEREST_INCOME}/{@code LOAN_INTEREST_EXPENSE} when set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interest_account_id")
    private AccountEntity interestAccount;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** The disbursement journal. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "write_off_journal_id")
    private JournalEntry writeOffJournal;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<LoanAmortizationLine> schedule = new ArrayList<>();

    private LocalDateTime approvedAt;

    private LocalDateTime disbursedAt;

    private LocalDateTime closedAt;

    private LocalDateTime cancelledAt;

    private LocalDateTime defaultedAt;

    private LocalDateTime reversedAt;

    public BigDecimal getInstallmentFee() {
        return installmentFee != null ? installmentFee : BigDecimal.ZERO;
    }

    public BigDecimal getAccruedInterestTotal() {
        return accruedInterestTotal != null ? accruedInterestTotal : BigDecimal.ZERO;
    }

    public BigDecimal getOverpaymentBalance() {
        return overpaymentBalance != null ? overpaymentBalance : BigDecimal.ZERO;
    }

    public BigDecimal getWrittenOffAmount() {
        return writtenOffAmount != null ? writtenOffAmount : BigDecimal.ZERO;
    }

    public int getGracePeriodInstallments() {
        return gracePeriodInstallments != null ? gracePeriodInstallments : 0;
    }

    public boolean isOverpaymentAllowed() {
        return Boolean.TRUE.equals(allowOverpayment);
    }
}
