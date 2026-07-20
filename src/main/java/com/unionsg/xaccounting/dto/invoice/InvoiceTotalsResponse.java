package com.unionsg.xaccounting.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTotalsResponse {

    private BigDecimal paidTotal = BigDecimal.ZERO;

    private BigDecimal overdueTotal = BigDecimal.ZERO;

    private BigDecimal pendingTotal = BigDecimal.ZERO;

    private BigDecimal grandTotal = BigDecimal.ZERO;
}
