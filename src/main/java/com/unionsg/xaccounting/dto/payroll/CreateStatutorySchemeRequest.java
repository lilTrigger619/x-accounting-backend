package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.StatutoryCalculationBasis;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateStatutorySchemeRequest {
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
    private String description;
}
