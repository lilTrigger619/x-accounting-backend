package com.unionsg.xaccounting.dto.bill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class BillTotalsResponse {

    private SummaryItem paid;
    private SummaryItem overdue;
    private SummaryItem pending;
    private SummaryItem grandTotal;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryItem {
        private Long count;
        private BigDecimal amount;
    }
}
