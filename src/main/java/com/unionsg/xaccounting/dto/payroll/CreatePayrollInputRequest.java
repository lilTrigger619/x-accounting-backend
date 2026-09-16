package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayrollInputSourceType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreatePayrollInputRequest {
    private Long employeeId;
    private Long payrollCalendarPeriodId;
    private PayrollInputSourceType sourceType;
    private Long payComponentId;
    private String description;
    private BigDecimal amount;
    private BigDecimal hours;
    private BigDecimal rate;
    private BigDecimal multiplier;
}
