package com.unionsg.xaccounting.repository.analytics;

import java.math.BigDecimal;

public interface AnalyticsBalanceRow {
    Long getAccountId();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
