package com.unionsg.xaccounting.dto.payroll;

import com.unionsg.xaccounting.enums.PayComponentCalculationMethod;
import com.unionsg.xaccounting.enums.PayComponentCategory;
import com.unionsg.xaccounting.enums.PayComponentSide;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreatePayComponentRequest {
    private String code;
    private String name;
    private PayComponentCategory category;
    private PayComponentSide side;
    private PayComponentCalculationMethod calculationMethod;
    private BigDecimal defaultValue;
    private boolean taxable = true;
    private boolean pensionable = true;
    private boolean statutory = false;
    private boolean recurring = true;
    private String glDebitAccountCode;
    private String glCreditAccountCode;
    private String description;
}
