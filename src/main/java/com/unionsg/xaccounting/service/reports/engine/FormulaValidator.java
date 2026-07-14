package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import java.util.List;
import java.util.Map;

public interface FormulaValidator {

    void validate(
            String sectionCode,
            String formula,
            Map<String, ReportSectionView> allSectionsByCode,
            List<ReportSectionView> sectionsInGraph,
            Map<String, java.math.BigDecimal> baseByCode
    );
}


