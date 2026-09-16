package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An employee loan (§15). Disbursement is recorded as an Employee Loan Receivable asset, not an
 * expense - the money is still the company's, merely lent out. Each payroll repayment reduces
 * {@code outstandingPrincipal}/{@code outstandingInterest} and the corresponding receivable
 * balance in the GL; the two are tracked separately because a loan's interest income (if any) has
 * a distinct accounting treatment from principal repayment (§15).
 */
@Entity
@Table(name = "employee_loans")
@Getter
@Setter
public class EmployeeLoan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private BigDecimal principal;

    private BigDecimal interestRatePercent = BigDecimal.ZERO;

    private LocalDate startDate;

    private LocalDate endDate;

    private BigDecimal installmentAmount;

    private BigDecimal outstandingPrincipal;

    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.ACTIVE;

    /** The journal that recorded the disbursement (Dr Employee Loan Receivable / Cr Bank). */
    private Long disbursementJournalId;

    private String reason;
}
