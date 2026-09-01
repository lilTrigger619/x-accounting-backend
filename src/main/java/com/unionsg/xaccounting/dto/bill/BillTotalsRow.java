package com.unionsg.xaccounting.dto.bill;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BillTotalsRow {

    private BigDecimal paidAmount;
    private Long paidCount;
    private BigDecimal overdueAmount;
    private Long overdueCount;
    private BigDecimal pendingAmount;
    private Long pendingCount;
    private BigDecimal grandAmount;
    private Long grandCount;

    public BillTotalsRow(
            BigDecimal paidAmount, Long paidCount,
            BigDecimal overdueAmount, Long overdueCount,
            BigDecimal pendingAmount, Long pendingCount,
            BigDecimal grandAmount, Long grandCount
    ) {
        this.paidAmount = paidAmount;
        this.paidCount = paidCount;
        this.overdueAmount = overdueAmount;
        this.overdueCount = overdueCount;
        this.pendingAmount = pendingAmount;
        this.pendingCount = pendingCount;
        this.grandAmount = grandAmount;
        this.grandCount = grandCount;
    }
}
