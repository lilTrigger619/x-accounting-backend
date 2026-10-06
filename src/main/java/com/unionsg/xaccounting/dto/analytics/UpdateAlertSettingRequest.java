package com.unionsg.xaccounting.dto.analytics;

import java.math.BigDecimal;

public record UpdateAlertSettingRequest(Boolean enabled, BigDecimal threshold, Integer windowDays) {
}
