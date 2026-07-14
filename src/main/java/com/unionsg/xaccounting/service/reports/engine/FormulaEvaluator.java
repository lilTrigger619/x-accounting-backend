package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface FormulaEvaluator {
    Map<String, BigDecimal> evaluate(Map<String, BigDecimal> baseByCode,
                                      List<ReportSectionView> sections);
}


