package com.unionsg.xaccounting.dto.analytics;

import java.util.List;

public record RevenueAnalyticsResponse(
        AnalyticsScope scope,
        MetricDto total,
        List<SeriesPoint> series,
        List<Breakdown> breakdowns,
        List<String> notes
) {
}
