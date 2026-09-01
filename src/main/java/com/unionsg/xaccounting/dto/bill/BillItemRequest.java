package com.unionsg.xaccounting.dto.bill;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BillItemRequest {
    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal taxRate;

}
