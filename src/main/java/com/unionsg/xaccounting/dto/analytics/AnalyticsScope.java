package com.unionsg.xaccounting.dto.analytics;

import java.util.List;

/**
 * Exactly what every number in a BI response was computed over. One scope per response: no
 * card can quietly use a different range or definition.
 */
public record AnalyticsScope(
        Period period,
        Period comparison,
        CompareMode compareMode,
        Granularity granularity,
        String baseCurrency,
        List<String> appliedFilters,
        List<IgnoredFilter> ignoredFilters,
        String basis
) {
}
