package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import com.unionsg.xaccounting.service.reports.engine.FormulaValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReportTemplateSectionFormulaValidator {

    private final FormulaValidator formulaValidator;

    // public void validateFormula(ReportSection section,
    public void validateFormula(ReportTemplateSection section,
                                  Map<String, ReportSectionView> allByCode,
                                  List<ReportSectionView> sectionsInGraph,

                                  Map<String, BigDecimal> baseByCode) {
        // Delegates to FormulaValidator which handles:
        // - invalid syntax
        // - unknown codes
        // - self ref
        // - circular refs
        // - division by zero (via shared evaluator)
        formulaValidator.validate(
                section.getSectionCode(),
                section.getFormula(),
                allByCode,
                sectionsInGraph,
                baseByCode
        );

    }

    
}

