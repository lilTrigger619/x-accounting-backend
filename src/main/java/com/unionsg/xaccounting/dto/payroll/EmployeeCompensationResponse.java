package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class EmployeeCompensationResponse {
    private Long id;
    private Long salaryStructureId;
    private String salaryStructureName;
    private BigDecimal basicSalary;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean current;
    private String reason;
}
