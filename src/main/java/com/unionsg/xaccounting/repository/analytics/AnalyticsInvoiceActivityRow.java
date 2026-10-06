package com.unionsg.xaccounting.repository.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AnalyticsInvoiceActivityRow {
    Long getAccountId();
    LocalDate getDay();
    Long getInvoiceId();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
