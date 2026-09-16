package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TaxBracketRequest {
    private Integer lineNumber;
    private BigDecimal minIncome;
    private BigDecimal maxIncome;
    private BigDecimal ratePercent;
}
