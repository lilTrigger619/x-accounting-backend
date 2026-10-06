package com.unionsg.xaccounting.repository.analytics;

import com.unionsg.xaccounting.enums.AccountType;

public interface AnalyticsAccountRow {
    Long getAccountId();
    String getAccountCode();
    String getAccountName();
    AccountType getAccountType();
    Long getChartCode();
    String getChartName();
    Long getClearToCode();
    String getClearToName();
}
