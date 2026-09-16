package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayComponentCategory;
import com.unionsg.xaccounting.enums.PayComponentSide;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SalaryStructureLineResponse {
    private Long id;
    private Long payComponentId;
    private String payComponentName;
    private PayComponentCategory category;
    private PayComponentSide side;
    private BigDecimal value;
}
