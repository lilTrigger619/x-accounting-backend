package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TaxBracketResponse {
    private Long id;
    private Integer lineNumber;
    private BigDecimal minIncome;
    private BigDecimal maxIncome;
    private BigDecimal ratePercent;
}
