package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.AdvanceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A salary advance (§14): cash paid to an employee ahead of the normal payroll date. Recorded as
 * an employee receivable, not an additional salary expense, at issuance. When a subsequent
 * payroll run deducts the repayment from net pay, the receivable is settled - the advance is
 * never expensed a second time (§49).
 */
@Entity
@Table(name = "salary_advances")
@Getter
@Setter
public class SalaryAdvance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private BigDecimal amountIssued;

    private LocalDate dateIssued;

    private BigDecimal outstandingBalance;

    /** Amount deducted from net pay each payroll run until fully recovered, if fixed. */
    private BigDecimal recurringDeductionAmount;

    @Enumerated(EnumType.STRING)
    private AdvanceStatus status = AdvanceStatus.OUTSTANDING;

    /** The journal that recorded the issuance (Dr Salary Advance Receivable / Cr Bank). */
    private Long issuanceJournalId;

    private String reason;
}
