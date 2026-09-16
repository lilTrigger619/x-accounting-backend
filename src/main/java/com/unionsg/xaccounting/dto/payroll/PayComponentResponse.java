package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayComponentCalculationMethod;
import com.unionsg.xaccounting.enums.PayComponentCategory;
import com.unionsg.xaccounting.enums.PayComponentSide;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayComponentResponse {
    private Long id;
    private String code;
    private String name;
    private PayComponentCategory category;
    private PayComponentSide side;
    private PayComponentCalculationMethod calculationMethod;
    private BigDecimal defaultValue;
    private boolean taxable;
    private boolean pensionable;
    private boolean statutory;
    private boolean recurring;
    private boolean active;
    private String glDebitAccountCode;
    private String glCreditAccountCode;
    private String description;
}
