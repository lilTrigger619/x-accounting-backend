package com.unionsg.xaccounting.entity.loan;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.loan.LoanPaymentStatus;
import com.unionsg.xaccounting.enums.loan.LoanPaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One payment against a {@link Loan}, split into principal, interest and fees. The split is
 * fixed when the payment is recorded (it is what the journal posted), so replaying payments
 * after a reversal always reproduces the same allocation.
 */
@Entity
@Table(name = "loan_repayments")
@Getter
@Setter
public class LoanRepayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "repayment_date", nullable = false)
    private LocalDate repaymentDate;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount = BigDecimal.ZERO;

    @Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount = BigDecimal.ZERO;

    @Column(name = "fees_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal feesAmount = BigDecimal.ZERO;

    /** Paid beyond the whole outstanding balance; posted to the principal account. */
    @Column(name = "overpayment_amount", precision = 19, scale = 2)
    private BigDecimal overpaymentAmount = BigDecimal.ZERO;

    /** The part of {@code interestAmount} that settled interest accrued earlier. */
    @Column(name = "accrued_interest_applied", precision = 19, scale = 2)
    private BigDecimal accruedInterestApplied = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 20)
    private LoanPaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private LoanPaymentStatus status = LoanPaymentStatus.POSTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(length = 500)
    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_journal_id")
    private JournalEntry reversalJournal;

    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    public BigDecimal getOverpaymentAmount() {
        return overpaymentAmount != null ? overpaymentAmount : BigDecimal.ZERO;
    }

    public BigDecimal getAccruedInterestApplied() {
        return accruedInterestApplied != null ? accruedInterestApplied : BigDecimal.ZERO;
    }

    public boolean isPosted() {
        return status == null || status == LoanPaymentStatus.POSTED;
    }
}
