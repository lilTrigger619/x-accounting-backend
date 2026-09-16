package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateEmployeeLoanRequest {
    private Long employeeId;
    private BigDecimal principal;
    private BigDecimal interestRatePercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal installmentAmount;
    private String reason;
}
