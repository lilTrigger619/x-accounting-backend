package com.unionsg.xaccounting.dto.analytics;

import java.util.List;

public record ExecutiveDashboardResponse(
        AnalyticsScope scope,
        List<MetricDto> kpis,
        List<TrendDto> trends,
        List<AlertDto> alerts,
        List<String> notes
) {
}
