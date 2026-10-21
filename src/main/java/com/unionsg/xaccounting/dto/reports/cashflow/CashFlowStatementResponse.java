package com.unionsg.xaccounting.dto.reports.cashflow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * An indirect-method cash flow statement for {@code [fromDate, toDate]}. {@code netCashFlow} is the
 * sum of the three sections and {@code cashChange} the movement on the cash and bank accounts
 * themselves; {@code difference} between them should always be zero.
 */
public record CashFlowStatementResponse(
        LocalDate fromDate,
        LocalDate toDate,
        BigDecimal netProfit,
        List<Section> sections,
        BigDecimal netCashFlow,
        BigDecimal openingCash,
        BigDecimal closingCash,
        BigDecimal cashChange,
        BigDecimal difference,
        List<AccountAmount> cashAccounts,
        List<String> notes
) {

    public record Section(String key, String title, List<Line> lines, BigDecimal total) {
    }

    /** One statement line with the accounts behind it, for drill-down. */
    public record Line(String key, String label, BigDecimal amount, List<AccountAmount> accounts) {
    }

    public record AccountAmount(Long accountId, String accountCode, String accountName, BigDecimal amount,
                                BigDecimal openingBalance, BigDecimal closingBalance) {
    }
}
