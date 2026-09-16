package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayrollRunStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PayrollRunResponse {
    private Long id;
    private String runNumber;
    private Long payrollCalendarPeriodId;
    private String payrollGroupName;
    private String periodLabel;
    private PayrollRunStatus status;

    private String preparedByName;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String approvedByName;
    private LocalDateTime approvedAt;

    private Integer employeeCount;
    private BigDecimal totalGrossPay;
    private BigDecimal totalEmployeeDeductions;
    private BigDecimal totalNetPay;
    private BigDecimal totalEmployerCost;

    private Long journalId;
    private LocalDateTime postedAt;
    private Long paymentJournalId;
    private LocalDateTime paidAt;

    private LocalDateTime reversedAt;
    private String reversalReason;
    private Long reversalOfRunId;

    private List<EmployeePayrollRecordResponse> records;
}
