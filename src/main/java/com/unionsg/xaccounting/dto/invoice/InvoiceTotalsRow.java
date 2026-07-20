package com.unionsg.xaccounting.dto.invoice;

import java.math.BigDecimal;

public class InvoiceTotalsRow {

    private final BigDecimal paidAmount;
    private final Long paidCount;
    private final BigDecimal overdueAmount;
    private final Long overdueCount;
    private final BigDecimal pendingAmount;
    private final Long pendingCount;
    private final BigDecimal grandAmount;
    private final Long grandCount;

    public InvoiceTotalsRow(
            BigDecimal paidAmount, Long paidCount,
            BigDecimal overdueAmount, Long overdueCount,
            BigDecimal pendingAmount, Long pendingCount,
            BigDecimal grandAmount, Long grandCount
    ) {
        this.paidAmount = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        this.paidCount = paidCount != null ? paidCount : 0L;
        this.overdueAmount = overdueAmount != null ? overdueAmount : BigDecimal.ZERO;
        this.overdueCount = overdueCount != null ? overdueCount : 0L;
        this.pendingAmount = pendingAmount != null ? pendingAmount : BigDecimal.ZERO;
        this.pendingCount = pendingCount != null ? pendingCount : 0L;
        this.grandAmount = grandAmount != null ? grandAmount : BigDecimal.ZERO;
        this.grandCount = grandCount != null ? grandCount : 0L;
    }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public Long getPaidCount() { return paidCount; }
    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public Long getOverdueCount() { return overdueCount; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public Long getPendingCount() { return pendingCount; }
    public BigDecimal getGrandAmount() { return grandAmount; }
    public Long getGrandCount() { return grandCount; }
}
