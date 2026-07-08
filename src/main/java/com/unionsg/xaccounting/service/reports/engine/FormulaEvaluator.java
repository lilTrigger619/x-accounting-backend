package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.reports.ReportSection;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FormulaEvaluator {
    Map<String, BigDecimal> evaluate(Map<String, BigDecimal> baseByCode,
                                      List<ReportSection> sections);
}

