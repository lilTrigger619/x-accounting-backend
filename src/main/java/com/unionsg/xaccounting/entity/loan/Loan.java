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
import com.unionsg.xaccounting.enums.loan.LoanInterestType;
import com.unionsg.xaccounting.enums.loan.LoanRepaymentMethod;
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
 * The Loan master record (Loans spec §13): covers both a loan the organization borrows
 * (direction {@code BORROWED} - from a bank, financial institution, shareholder, director or
 * other lender) and a loan the organization lends out (direction {@code LENT} - to an employee,
 * customer, supplier or other party). Which side of the balance sheet a disbursement/repayment
 * hits is entirely driven by {@code direction} (§15/§16).
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

    @Column(length = 10)
    private String currency = "USD";

    @Column(name = "interest_rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal interestRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type", nullable = false, length = 20)
    private LoanInterestType interestType = LoanInterestType.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_method", nullable = false, length = 20)
    private LoanInterestMethod interestMethod = LoanInterestMethod.SIMPLE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_frequency", nullable = false, length = 20)
    private LoanFrequency paymentFrequency;

    @Column(name = "number_of_installments", nullable = false)
    private Integer numberOfInstallments;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_method", nullable = false, length = 20)
    private LoanRepaymentMethod repaymentMethod;

    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingPrincipal = BigDecimal.ZERO;

    @Column(name = "outstanding_interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    @Column(name = "total_fees", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFees = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_treatment", length = 30)
    private LoanFeeTreatment feeTreatment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status = LoanStatus.DRAFT;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("installmentNumber ASC")
    private List<LoanAmortizationLine> schedule = new ArrayList<>();

    private LocalDateTime approvedAt;

    private LocalDateTime disbursedAt;

    private LocalDateTime closedAt;

    private LocalDateTime cancelledAt;
}
