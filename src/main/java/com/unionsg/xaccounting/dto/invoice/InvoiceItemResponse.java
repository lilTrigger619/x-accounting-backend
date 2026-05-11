package com.unionsg.xaccounting.dto.invoice;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InvoiceItemResponse {

    private Long id;

    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal taxRate;

    private BigDecimal lineSubtotal;

    private BigDecimal lineTax;

    private BigDecimal lineTotal;

}
