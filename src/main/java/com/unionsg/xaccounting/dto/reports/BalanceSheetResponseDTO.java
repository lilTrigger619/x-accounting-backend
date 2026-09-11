package com.unionsg.xaccounting.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A classic Assets = Liabilities + Equity statement as of a single date.
 *
 * <p>{@code currentYearEarnings} is the net income accumulated since the current Financial
 * Year's start date (or, if none is set up yet, since the start of the calendar year) that has
 * not yet been swept into Retained Earnings by a Year-End Closing - without it, the balance sheet
 * would not balance for any date that falls mid-year. {@code balanceCheck} is
 * {@code totalAssets - (totalLiabilities + totalEquityAndCurrentEarnings)} and should always be
 * (near) zero; a nonzero value indicates a data-integrity problem in the ledger.</p>
 */
public record BalanceSheetResponseDTO(

        LocalDate asOfDate,

        List<BalanceSheetAccountDto> assets,
        BigDecimal totalAssets,

        List<BalanceSheetAccountDto> liabilities,
        BigDecimal totalLiabilities,

        List<BalanceSheetAccountDto> equity,
        BigDecimal totalEquity,

        BigDecimal currentYearEarnings,
        BigDecimal totalEquityAndCurrentEarnings,

        BigDecimal totalLiabilitiesAndEquity,

        BigDecimal balanceCheck

) {
}
