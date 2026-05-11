package com.unionsg.xaccounting.dto.invoice;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceItemRequest {
    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal taxRate;

}
