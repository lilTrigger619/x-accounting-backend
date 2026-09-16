package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.ReimbursementStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * An employee expense reimbursement claim (§16): Claim -> Approval -> Payroll/payment ->
 * Accounting. Distinguished from salary earnings on the payslip and in the GL. {@code
 * alreadyRecordedElsewhere} guards against double expense recognition when the underlying cost
 * was already booked through a Bill or an Expense elsewhere in the system - in that case this
 * claim only drives the payroll cash-out, not a second expense entry (§16, §49).
 */
@Entity
@Table(name = "reimbursement_claims")
@Getter
@Setter
public class ReimbursementClaim extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private String description;

    private String category;

    private BigDecimal amount;

    private LocalDate claimDate;

    @Enumerated(EnumType.STRING)
    private ReimbursementStatus status = ReimbursementStatus.PENDING;

    /** True when the expense itself was already booked elsewhere (e.g. a Bill) - see class doc. */
    private boolean alreadyRecordedElsewhere = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;
}
