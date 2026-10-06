package com.unionsg.xaccounting.dto.analytics;

import java.util.List;

/** A named split of a total. {@code supported=false} rows carry the reason instead of data. */
public record Breakdown(String key, String label, boolean supported, String reason, List<BreakdownRow> rows) {
}
