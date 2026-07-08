package com.unionsg.xaccounting.service.reports.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface JournalPostedBalanceRepository {
    Map<Long, BigDecimal> sumPostedBalancesByAccountId(List<Long> accountIds, LocalDate from, LocalDate to);
}

