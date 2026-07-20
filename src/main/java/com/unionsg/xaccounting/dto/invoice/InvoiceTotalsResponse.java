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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryItem {
        private Long count = 0L;
        private BigDecimal amount = BigDecimal.ZERO;
    }

    private SummaryItem paid;

    private SummaryItem overdue;

    private SummaryItem pending;

    private SummaryItem grandTotal;
}
