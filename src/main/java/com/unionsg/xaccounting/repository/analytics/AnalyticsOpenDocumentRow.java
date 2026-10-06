package com.unionsg.xaccounting.repository.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AnalyticsOpenDocumentRow {
    Long getId();
    LocalDate getDueDate();
    BigDecimal getBalance();
    String getCurrency();
}
