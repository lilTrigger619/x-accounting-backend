package com.unionsg.xaccounting.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One bucket of a trend; {@code previous} is the matching bucket of the comparison period. */
public record SeriesPoint(String label, LocalDate start, LocalDate end, BigDecimal current,
                          String previousLabel, BigDecimal previous, BigDecimal growthPct) {
}
