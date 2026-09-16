package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.StatutoryCalculationBasis;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class StatutorySchemeResponse {
    private Long id;
    private String code;
    private String name;
    private StatutoryCalculationBasis calculationBasis;
    private BigDecimal employeeRatePercent;
    private BigDecimal employerRatePercent;
    private String employeeLiabilityAccountCode;
    private String employerExpenseAccountCode;
    private String employerLiabilityAccountCode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private boolean active;
    private String description;
}
