package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.PayComponentSide;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * One itemized line of an employee's payroll record - what a Payslip is built from (§39). {@code
 * componentName} is a snapshot of the label at calculation time, deliberately denormalized so a
 * later rename of a PayComponent or StatutoryScheme never changes how a historical payslip reads
 * (§59).
 */
@Entity
@Table(name = "payroll_record_components")
@Getter
@Setter
public class PayrollRecordComponent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_payroll_record_id", nullable = false)
    private EmployeePayrollRecord employeePayrollRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_component_id")
    private PayComponent payComponent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statutory_scheme_id")
    private StatutoryScheme statutoryScheme;

    private String componentName;

    @Enumerated(EnumType.STRING)
    private PayComponentSide side;

    private BigDecimal amount;

    /**
     * Free-form tag used only for the run-level built-ins that have neither a PayComponent nor a
     * StatutoryScheme (a loan/advance repayment line) - e.g. "EMPLOYEE_LOAN:42" - so
     * {@code PayrollJournalService} can resolve the right GL account and update the right
     * subledger balance after posting. Not shown on the payslip.
     */
    private String description;
}
