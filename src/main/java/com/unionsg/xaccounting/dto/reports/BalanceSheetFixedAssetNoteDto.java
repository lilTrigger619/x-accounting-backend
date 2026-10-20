package com.unionsg.xaccounting.dto.reports;

import java.math.BigDecimal;
import java.util.List;

/**
 * Property, plant and equipment note under the Balance Sheet: the same GL balances the asset lines
 * already include, regrouped as cost less accumulated depreciation and impairment. It adds nothing
 * to the statement totals. {@code subledgerCarryingAmount} is the fixed asset register's figure and
 * {@code difference} the gap between the two.
 */
public record BalanceSheetFixedAssetNoteDto(
        List<Line> costLines,
        BigDecimal totalCost,
        List<Line> accumulatedDepreciationLines,
        BigDecimal totalAccumulatedDepreciation,
        List<Line> accumulatedImpairmentLines,
        BigDecimal totalAccumulatedImpairment,
        BigDecimal netCarryingAmount,
        BigDecimal subledgerCarryingAmount,
        BigDecimal difference
) {
    public record Line(Long accountId, String accountCode, String accountName, BigDecimal amount) {
    }
}
