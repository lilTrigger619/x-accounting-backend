package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayFrequency;
import com.unionsg.xaccounting.enums.PayrollPeriodStatus;
import com.unionsg.xaccounting.enums.PayrollRunStatus;
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
    /** The latest payroll run on this period that wasn't cancelled; null when there is none. */
    private Long payrollRunId;
    private String payrollRunNumber;
    private PayrollRunStatus payrollRunStatus;
}
