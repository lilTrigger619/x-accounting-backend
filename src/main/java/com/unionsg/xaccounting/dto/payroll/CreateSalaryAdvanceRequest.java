package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateSalaryAdvanceRequest {
    private Long employeeId;
    private BigDecimal amountIssued;
    private LocalDate dateIssued;
    private BigDecimal recurringDeductionAmount;
    private String reason;
}
