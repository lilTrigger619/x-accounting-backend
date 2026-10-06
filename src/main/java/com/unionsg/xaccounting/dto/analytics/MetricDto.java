package com.unionsg.xaccounting.dto.analytics;

import java.math.BigDecimal;

/**
 * One KPI. {@code change} is absolute (percentage points for ratios); {@code changePct} is null
 * when the previous value is zero or the metric is itself a ratio.
 */
public record MetricDto(
        String key,
        String label,
        String format,
        BigDecimal current,
        BigDecimal previous,
        BigDecimal change,
        BigDecimal changePct,
        String direction,
        String sentiment,
        String explanation,
        String definition,
        DrillTarget drill
) {
}
