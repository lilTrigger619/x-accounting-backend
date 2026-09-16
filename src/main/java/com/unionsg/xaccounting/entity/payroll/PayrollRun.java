package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.PayrollRunStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A single payroll processing run for one Payroll Calendar Period (§20). The status enum is the
 * whole point of this entity: it is the mechanism that keeps calculation, review, approval,
 * accounting and payment as five distinct, separately-authorized events (the central requirement
 * of this module) rather than one opaque "run payroll" button. Reaching {@code CALCULATED} never
 * implies employees have been paid, or even that the GL has been touched - only {@code POSTED}
 * means a journal exists, and only {@code PAID} means cash has actually moved.
 */
@Entity
@Table(name = "payroll_runs")
@Getter
@Setter
public class PayrollRun extends BaseEntity {

    @Column(unique = true)
    private String runNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_calendar_period_id", nullable = false)
    private PayrollCalendarPeriod payrollCalendarPeriod;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_group_id", nullable = false)
    private PayrollGroup payrollGroup;

    @Enumerated(EnumType.STRING)
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    // ===== Segregation of duties (§22, §45) =====

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_by")
    private User preparedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private LocalDateTime reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    // ===== Totals, snapshotted at CALCULATED (§21) =====

    private BigDecimal totalGrossPay = BigDecimal.ZERO;

    private BigDecimal totalEmployeeDeductions = BigDecimal.ZERO;

    private BigDecimal totalNetPay = BigDecimal.ZERO;

    private BigDecimal totalEmployerCost = BigDecimal.ZERO;

    private Integer employeeCount = 0;

    // ===== Accounting (§23-§26) =====

    /** The PAYROLL-type journal recognizing salary expense and Salary/Statutory Payable. */
    private Long journalId;

    private LocalDateTime postedAt;

    // ===== Payment (§27) =====

    /** The GENERAL-type journal moving Salary Payable to Bank. */
    private Long paymentJournalId;

    private LocalDateTime paidAt;

    // ===== Reversal (§30) =====

    private LocalDateTime reversedAt;

    private String reversalReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_of_run_id")
    private PayrollRun reversalOfRun;

    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmployeePayrollRecord> records = new ArrayList<>();
}
