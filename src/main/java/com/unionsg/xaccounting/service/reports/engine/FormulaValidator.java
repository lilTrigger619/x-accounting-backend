package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.reports.ReportSection;

import java.util.List;
import java.util.Map;

public interface FormulaValidator {

    void validate(
            String sectionCode,
            String formula,
            Map<String, ReportSection> allSectionsByCode,
            List<ReportSection> sectionsInGraph,
            Map<String, java.math.BigDecimal> baseByCode
    );
}

