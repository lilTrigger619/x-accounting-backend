package com.unionsg.xaccounting.service.reports.template.validation.impl;

import com.unionsg.xaccounting.dto.reports.ValidationErrorDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.enums.ReportTemplateValidationSeverity;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.service.reports.engine.FormulaValidator;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;
import com.unionsg.xaccounting.service.reports.engine.view.impl.SimpleReportSectionView;
import com.unionsg.xaccounting.service.reports.template.validation.FormulaValidatorAdapter;
import com.unionsg.xaccounting.service.reports.template.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FormulaValidatorAdapterImpl implements FormulaValidatorAdapter {

    private final ReportTemplateSectionRepository sectionRepository;
    private final FormulaValidator formulaValidator;

    @Override
    @Transactional(readOnly = true)
    public void validate(ReportTemplate template, ValidationResult result) {
        List<ReportTemplateSection> sections = sectionRepository.findByReportTemplateId(template.getId());
        if (sections == null || sections.isEmpty()) return;

        Map<String, ReportSectionView> allByCode = new HashMap<>();
        List<ReportSectionView> sectionsInGraph = new ArrayList<>();
        Map<String, BigDecimal> baseByCode = new HashMap<>();

        for (ReportTemplateSection s : sections) {
            String parentCode = s.getParentSection() == null ? null : s.getParentSection().getSectionCode();
            ReportSectionView view = new SimpleReportSectionView(
                    s.getId(),
                    s.getSectionCode(),
                    s.getTitle(),
                    s.getSectionType(),
                    s.getFormula(),
                    s.getDisplayOrder(),
                    parentCode,
                    List.of()
            );

            allByCode.put(s.getSectionCode(), view);
            sectionsInGraph.add(view);
            baseByCode.put(s.getSectionCode(), BigDecimal.ZERO);
        }

        for (ReportTemplateSection s : sections) {
            SectionType type = s.getSectionType();
            String code = s.getSectionCode();

            String formula = s.getFormula();
            boolean hasFormula = formula != null && !formula.trim().isEmpty();

            if (type == null) continue;

            // SectionType rules (spec-driven)
            switch (type) {
                case GROUP -> {
                    if (hasFormula) {
                        result.addError(new ValidationErrorDto(
                                "GROUP_CONTAINS_FORMULA",
                                "The '" + code + "' section must NOT contain formulas because it is a GROUP (structural heading).",
                                code,
                                code,
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                    continue;
                }
                case DETAIL -> {
                    if (hasFormula) {
                        result.addError(new ValidationErrorDto(
                                "DETAIL_CONTAINS_FORMULA",
                                "The '" + code + "' section must NOT contain formulas because it is a DETAIL (leaf).",
                                code,
                                code,
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                    continue;
                }
                case SUBTOTAL, TOTAL -> {
                    if (!hasFormula) {
                        result.addError(new ValidationErrorDto(
                                type == SectionType.SUBTOTAL ? "SUBTOTAL_MISSING_FORMULA" : "TOTAL_MISSING_FORMULA",
                                "The '" + code + "' section must contain a formula.",
                                code,
                                code,
                                ReportTemplateValidationSeverity.ERROR
                        ));
                        continue;
                    }
                }
                default -> {
                }
            }


            if (hasFormula) {
                try {
                    formulaValidator.validate(code, formula, allByCode, sectionsInGraph, baseByCode);
                } catch (RuntimeException ex) {
                    result.addError(new ValidationErrorDto(
                            "FORMULA_INVALID",
                            ex.getMessage(),
                            code,
                            code,
                            ReportTemplateValidationSeverity.ERROR
                    ));
                }
            }
        }
    }
}

