package com.unionsg.xaccounting.repository.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AnalyticsDailyRow {
    Long getAccountId();
    LocalDate getDay();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
