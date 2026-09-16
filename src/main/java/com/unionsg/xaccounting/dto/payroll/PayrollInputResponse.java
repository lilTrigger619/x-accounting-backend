package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayrollInputSourceType;
import com.unionsg.xaccounting.enums.PayrollInputStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayrollInputResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long payrollCalendarPeriodId;
    private PayrollInputSourceType sourceType;
    private Long payComponentId;
    private String payComponentName;
    private String description;
    private BigDecimal amount;
    private BigDecimal hours;
    private BigDecimal rate;
    private BigDecimal multiplier;
    private BigDecimal resolvedAmount;
    private PayrollInputStatus status;
}
