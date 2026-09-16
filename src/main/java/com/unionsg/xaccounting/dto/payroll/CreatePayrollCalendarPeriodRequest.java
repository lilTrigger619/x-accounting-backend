package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreatePayrollCalendarPeriodRequest {
    private Long payrollGroupId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate payDate;
    private LocalDate accountingDate;
}
