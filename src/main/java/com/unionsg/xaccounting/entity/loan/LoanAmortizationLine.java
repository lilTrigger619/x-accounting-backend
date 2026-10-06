package com.unionsg.xaccounting.entity.loan;

import com.unionsg.xaccounting.enums.loan.LoanInstallmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One installment of a {@link Loan}'s amortization schedule. The {@code original*} columns keep
 * the schedule as generated; the working columns change when an early payment re-amortizes the
 * remaining installments. Rebuilding a loan after a payment reversal starts again from the
 * originals and replays the payments still standing.
 */
@Entity
@Table(name = "loan_amortization_lines")
@Getter
@Setter
public class LoanAmortizationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "opening_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingPrincipal;

    @Column(name = "principal_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalDue;

    @Column(name = "interest_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestDue;

    @Column(name = "fees_due", precision = 19, scale = 2)
    private BigDecimal feesDue = BigDecimal.ZERO;

    /** Principal + interest + fees. */
    @Column(name = "total_installment", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInstallment;

    @Column(name = "closing_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal closingPrincipal;

    @Column(name = "principal_paid", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalPaid = BigDecimal.ZERO;

    @Column(name = "interest_paid", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @Column(name = "fees_paid", precision = 19, scale = 2)
    private BigDecimal feesPaid = BigDecimal.ZERO;

    @Column(name = "original_opening_principal", precision = 19, scale = 2)
    private BigDecimal originalOpeningPrincipal;

    @Column(name = "original_principal_due", precision = 19, scale = 2)
    private BigDecimal originalPrincipalDue;

    @Column(name = "original_interest_due", precision = 19, scale = 2)
    private BigDecimal originalInterestDue;

    @Column(name = "original_fees_due", precision = 19, scale = 2)
    private BigDecimal originalFeesDue;

    @Column(name = "original_closing_principal", precision = 19, scale = 2)
    private BigDecimal originalClosingPrincipal;

    /** Recorded by a user as missed. Cleared once the installment is paid in full. */
    @Column(name = "missed")
    private Boolean missed = false;

    @Column(name = "missed_note", length = 500)
    private String missedNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanInstallmentStatus status = LoanInstallmentStatus.PENDING;

    public BigDecimal getFeesDue() {
        return feesDue != null ? feesDue : BigDecimal.ZERO;
    }

    public BigDecimal getFeesPaid() {
        return feesPaid != null ? feesPaid : BigDecimal.ZERO;
    }

    public boolean isMissed() {
        return Boolean.TRUE.equals(missed);
    }

    public BigDecimal principalOwed() {
        return principalDue.subtract(principalPaid).max(BigDecimal.ZERO);
    }

    public BigDecimal interestOwed() {
        return interestDue.subtract(interestPaid).max(BigDecimal.ZERO);
    }

    public BigDecimal feesOwed() {
        return getFeesDue().subtract(getFeesPaid()).max(BigDecimal.ZERO);
    }

    public BigDecimal totalOwed() {
        return principalOwed().add(interestOwed()).add(feesOwed());
    }

    public BigDecimal totalPaid() {
        return principalPaid.add(interestPaid).add(getFeesPaid());
    }

    /** Copies the working amounts into the original columns. Called once when generated. */
    public void snapshotOriginals() {
        originalOpeningPrincipal = openingPrincipal;
        originalPrincipalDue = principalDue;
        originalInterestDue = interestDue;
        originalFeesDue = getFeesDue();
        originalClosingPrincipal = closingPrincipal;
    }

    /** Puts the line back as generated, unpaid. Rows from before the originals existed keep their amounts. */
    public void resetToOriginal() {
        if (originalPrincipalDue != null) {
            openingPrincipal = originalOpeningPrincipal;
            principalDue = originalPrincipalDue;
            interestDue = originalInterestDue;
            feesDue = originalFeesDue;
            closingPrincipal = originalClosingPrincipal;
        }
        totalInstallment = principalDue.add(interestDue).add(getFeesDue());
        principalPaid = BigDecimal.ZERO;
        interestPaid = BigDecimal.ZERO;
        feesPaid = BigDecimal.ZERO;
        status = LoanInstallmentStatus.PENDING;
    }

    /** Sets PAID / PARTIALLY_PAID / PENDING from the paid amounts; a full payment clears "missed". */
    public void refreshStatus() {
        if (totalOwed().signum() == 0) {
            status = LoanInstallmentStatus.PAID;
            missed = false;
        } else if (totalPaid().signum() > 0) {
            status = LoanInstallmentStatus.PARTIALLY_PAID;
        } else {
            status = LoanInstallmentStatus.PENDING;
        }
    }
}
