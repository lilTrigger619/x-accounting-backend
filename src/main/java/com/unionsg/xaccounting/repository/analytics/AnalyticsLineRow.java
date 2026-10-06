package com.unionsg.xaccounting.repository.analytics;

import com.unionsg.xaccounting.enums.JournalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AnalyticsLineRow {
    Long getLineId();
    Long getJournalId();
    String getJournalNumber();
    LocalDate getJournalDate();
    JournalStatus getStatus();
    String getJournalDescription();
    String getLineDescription();
    String getReference();
    String getSourceModule();
    Long getSourceEntityId();
    String getOriginalSourceModule();
    Long getOriginalSourceEntityId();
    Long getReversalOfJournalId();
    Long getAccountId();
    String getAccountCode();
    String getAccountName();
    BigDecimal getDebit();
    BigDecimal getCredit();
}
