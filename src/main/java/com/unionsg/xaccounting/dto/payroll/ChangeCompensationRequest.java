package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Assigns a new Salary Structure/basic salary to an employee effective from a given date (§6).
 * The employee's previously-current assignment is closed off (effectiveTo = day before) rather
 * than edited, preserving the compensation history a historical payroll run must resolve against.
 */
@Getter
@Setter
public class ChangeCompensationRequest {
    private Long salaryStructureId;
    private BigDecimal basicSalary;
    private LocalDate effectiveFrom;
    private String reason;
}
