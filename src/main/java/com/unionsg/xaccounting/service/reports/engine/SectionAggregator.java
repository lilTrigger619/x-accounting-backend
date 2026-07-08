package com.unionsg.xaccounting.service.reports.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface SectionAggregator {
    Map<String, BigDecimal> aggregate(Long reportDefinitionId, LocalDate from, LocalDate to);
}

