package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One employee's calculated result within a Payroll Run - the payroll subledger line that a
 * Payslip (§39) is generated from. {@code employeeSalaryStructure} is captured for traceability:
 * even if the employee's compensation changes later, this record still points at exactly which
 * assignment was used (§48, §59).
 */
@Entity
@Table(name = "employee_payroll_records")
@Getter
@Setter
public class EmployeePayrollRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_salary_structure_id")
    private EmployeeSalaryStructure employeeSalaryStructure;

    private BigDecimal grossPay = BigDecimal.ZERO;

    private BigDecimal totalEmployeeDeductions = BigDecimal.ZERO;

    private BigDecimal netPay = BigDecimal.ZERO;

    private BigDecimal totalEmployerCost = BigDecimal.ZERO;

    /** True if this record's net pay was negative and had to be clamped/flagged (§57). */
    private boolean negativeNetPayFlagged = false;

    @OneToMany(mappedBy = "employeePayrollRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollRecordComponent> components = new ArrayList<>();
}
