package com.unionsg.xaccounting.dto.analytics;

import java.math.BigDecimal;

/**
 * A management alert check. status is TRIGGERED, CLEAR, DISABLED, NOT_AVAILABLE or
 * INSUFFICIENT_DATA; severity (WARNING or CRITICAL) is only set when triggered.
 */
public record AlertDto(String key, String title, String status, String severity, String message,
                       BigDecimal value, BigDecimal threshold, String unit, DrillTarget drill) {
}
