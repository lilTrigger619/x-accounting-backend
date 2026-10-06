package com.unionsg.xaccounting.dto.analytics;

import java.math.BigDecimal;

public record AlertSettingDto(String key, String title, String description, boolean enabled,
                              BigDecimal threshold, String unit, Integer windowDays,
                              BigDecimal defaultThreshold, Integer defaultWindowDays,
                              boolean available, String updatedBy, java.time.LocalDateTime updatedAt) {
}
