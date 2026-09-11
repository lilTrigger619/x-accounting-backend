package com.unionsg.xaccounting.dto.dashboard;

public record AccountingHealthDto(

        String currentFinancialYearName,
        String currentFinancialYearStatus,
        long lockedPeriodsCount,
        long draftJournalsCount,
        boolean hasOpeningBalance

) {
}
