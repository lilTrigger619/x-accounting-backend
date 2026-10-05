package com.unionsg.xaccounting.dto.downpayment;

import lombok.Data;

import java.math.BigDecimal;

/** Unused downpayment balance for one customer/supplier, bucketed by days since payment. */
@Data
public class DownpaymentAgingRow {
    private Long counterpartyId;
    private String counterpartyName;
    private String currency;
    private BigDecimal days0To30 = BigDecimal.ZERO;
    private BigDecimal days31To60 = BigDecimal.ZERO;
    private BigDecimal days61To90 = BigDecimal.ZERO;
    private BigDecimal days91To180 = BigDecimal.ZERO;
    private BigDecimal over180 = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
}
