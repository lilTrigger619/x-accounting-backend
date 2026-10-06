package com.unionsg.xaccounting.dto.analytics;

import java.util.List;

public record TrendDto(String key, String label, String format, List<SeriesPoint> points, DrillTarget drill) {
}
