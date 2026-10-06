package com.unionsg.xaccounting.service.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.unionsg.xaccounting.service.analytics.AccountClass.*;

/**
 * The headline figures for one period, all from the same {@link LedgerData}: period figures are
 * activity inside [from, to]; balance figures are balances at the end of {@code to}.
 */
public record FinancialFigures(
        BigDecimal revenue,
        BigDecimal otherIncome,
        BigDecimal costOfSales,
        BigDecimal operatingExpenses,
        BigDecimal otherExpenses,
        BigDecimal cash,
        BigDecimal receivables,
        BigDecimal payables,
        BigDecimal loans,
        BigDecimal currentAssets,
        BigDecimal currentLiabilities
) {
    public static final List<AccountClass> CURRENT_ASSETS = List.of(CASH_BANK, RECEIVABLE, OTHER_CURRENT_ASSET);
    public static final List<AccountClass> CURRENT_LIABILITIES = List.of(PAYABLE, LOAN_LIABILITY, OTHER_CURRENT_LIABILITY);
    public static final List<AccountClass> ALL_EXPENSES = List.of(COST_OF_SALES, OPERATING_EXPENSE, OTHER_EXPENSE);

    public static FinancialFigures of(LedgerData data, LocalDate from, LocalDate to) {
        return new FinancialFigures(
                data.activity(List.of(OPERATING_REVENUE), from, to),
                data.activity(List.of(OTHER_INCOME), from, to),
                data.activity(List.of(COST_OF_SALES), from, to),
                data.activity(List.of(OPERATING_EXPENSE), from, to),
                data.activity(List.of(OTHER_EXPENSE), from, to),
                data.balanceAt(List.of(CASH_BANK), to),
                data.balanceAt(List.of(RECEIVABLE), to),
                data.balanceAt(List.of(PAYABLE), to),
                data.balanceAt(List.of(LOAN_LIABILITY), to),
                data.balanceAt(CURRENT_ASSETS, to),
                data.balanceAt(CURRENT_LIABILITIES, to));
    }

    public BigDecimal grossProfit() {
        return revenue.subtract(costOfSales);
    }

    public BigDecimal totalExpenses() {
        return costOfSales.add(operatingExpenses).add(otherExpenses);
    }

    public BigDecimal netProfit() {
        return revenue.add(otherIncome).subtract(totalExpenses());
    }

    public BigDecimal grossMargin() {
        return Metrics.pct(grossProfit(), revenue);
    }

    public BigDecimal netMargin() {
        return Metrics.pct(netProfit(), revenue);
    }

    public BigDecimal workingCapital() {
        return currentAssets.subtract(currentLiabilities);
    }
}
