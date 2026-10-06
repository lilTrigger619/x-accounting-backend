package com.unionsg.xaccounting.dto.analytics;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsContextResponse(
        LocalDate today,
        String baseCurrency,
        Period currentFinancialYear,
        Period currentAccountingPeriod,
        List<String> currencies,
        List<FilterSupport> filters,
        List<AccountOption> accounts
) {
    public record AccountOption(Long id, String code, String name, String accountClass, String classLabel) {
    }
}
