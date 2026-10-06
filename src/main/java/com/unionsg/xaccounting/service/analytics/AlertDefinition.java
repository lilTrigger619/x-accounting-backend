package com.unionsg.xaccounting.service.analytics;

import java.math.BigDecimal;

/**
 * The management alerts the executive dashboard checks. Each threshold is relative (a percentage
 * change, a share, months of cover), never a fixed amount, and every one can be changed or
 * switched off in BI settings.
 */
public enum AlertDefinition {
    REVENUE_DECLINE("Significant revenue decline",
            "Revenue fell by at least this percentage against the comparison period.",
            "%", new BigDecimal("10"), null),
    EXPENSE_INCREASE("Significant expense increase",
            "Total expenses (cost of sales, operating and other expenses) rose by at least this percentage against the comparison period.",
            "%", new BigDecimal("15"), null),
    LOW_CASH("Low cash position",
            "Cash and bank balances cover fewer than this many months of the period's average monthly expenses.",
            "months", new BigDecimal("1"), null),
    HIGH_OVERDUE_RECEIVABLES("High overdue receivables",
            "At least this percentage of open invoice balances is past its due date at the end of the period.",
            "%", new BigDecimal("30"), null),
    PAYABLE_OBLIGATIONS("Significant payable obligations",
            "Unpaid bills due by the end of the window (including overdue ones) reach at least this percentage of cash and bank balances.",
            "%", new BigDecimal("75"), 30),
    BUDGET_VARIANCE("Large budget variance",
            "Actual results differ from budget by at least this percentage.",
            "%", new BigDecimal("10"), null),
    PROFITABILITY_DECLINE("Significant profitability decline",
            "Net profit margin fell by at least this many percentage points against the comparison period.",
            "points", new BigDecimal("5"), null);

    private final String title;
    private final String description;
    private final String unit;
    private final BigDecimal defaultThreshold;
    private final Integer defaultWindowDays;

    AlertDefinition(String title, String description, String unit, BigDecimal defaultThreshold, Integer defaultWindowDays) {
        this.title = title;
        this.description = description;
        this.unit = unit;
        this.defaultThreshold = defaultThreshold;
        this.defaultWindowDays = defaultWindowDays;
    }

    public String title() { return title; }
    public String description() { return description; }
    public String unit() { return unit; }
    public BigDecimal defaultThreshold() { return defaultThreshold; }
    public Integer defaultWindowDays() { return defaultWindowDays; }

    /** Budget vs Actual has no data source yet, so that check reports itself unavailable. */
    public boolean available() {
        return this != BUDGET_VARIANCE;
    }
}
