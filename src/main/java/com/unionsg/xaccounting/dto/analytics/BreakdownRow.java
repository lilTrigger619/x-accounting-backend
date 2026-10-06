package com.unionsg.xaccounting.dto.analytics;

import java.math.BigDecimal;

public record BreakdownRow(String key, String label, BigDecimal current, BigDecimal previous,
                           BigDecimal change, BigDecimal changePct, BigDecimal share, DrillTarget drill) {
}
