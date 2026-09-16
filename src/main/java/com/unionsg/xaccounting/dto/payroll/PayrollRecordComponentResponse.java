package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayComponentSide;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayrollRecordComponentResponse {
    private String componentName;
    private PayComponentSide side;
    private BigDecimal amount;
}
