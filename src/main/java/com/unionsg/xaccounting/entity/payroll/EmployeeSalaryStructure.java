package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The effective-dated assignment of a Salary Structure and basic salary to one employee (§6).
 * A new row is created every time an employee's compensation changes - the previous row's
 * {@code effectiveTo} is set to the day before, and it is never edited or deleted, so a payroll
 * run for a historical period always resolves the compensation that was actually in force on
 * that period's pay date, even after later raises (§59).
 */
@Entity
@Table(name = "employee_salary_structures")
@Getter
@Setter
public class EmployeeSalaryStructure extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salary_structure_id", nullable = false)
    private SalaryStructure salaryStructure;

    private BigDecimal basicSalary;

    private LocalDate effectiveFrom;

    /** Null while this is the employee's current, open-ended compensation. */
    private LocalDate effectiveTo;

    private boolean current = true;

    /** e.g. "Annual increment", "Promotion", "New hire" - shown on the compensation history. */
    private String reason;
}
