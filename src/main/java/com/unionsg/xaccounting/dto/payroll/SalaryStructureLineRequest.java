package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SalaryStructureLineRequest {
    private Long payComponentId;
    private BigDecimal value;
}
