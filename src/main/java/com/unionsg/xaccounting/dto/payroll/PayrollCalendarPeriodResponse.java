package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayFrequency;
import com.unionsg.xaccounting.enums.PayrollPeriodStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class PayrollCalendarPeriodResponse {
    private Long id;
    private Long payrollGroupId;
    private String payrollGroupName;
    private PayFrequency payFrequency;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate payDate;
    private LocalDate accountingDate;
    private Long accountingPeriodId;
    private String accountingPeriodName;
    private Long financialYearId;
    private String financialYearName;
    private PayrollPeriodStatus status;
}
