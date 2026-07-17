package com.unionsg.xaccounting.service.reports.template.validation.impl;

import com.unionsg.xaccounting.dto.reports.ValidationErrorDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.enums.ReportTemplateValidationSeverity;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import com.unionsg.xaccounting.service.reports.template.validation.BusinessRuleValidator;
import com.unionsg.xaccounting.service.reports.template.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BusinessRuleValidatorImpl implements BusinessRuleValidator {

    private final ReportTemplateSectionRepository sectionRepository;

    @Override
    @Transactional(readOnly = true)
    public void validate(ReportTemplate template, ValidationResult result) {
        List<ReportTemplateSection> sections = sectionRepository.findByReportTemplateId(template.getId());
        if (sections == null || sections.isEmpty()) return;

        Map<Long, Long> childCountByParent = sections.stream()
                .filter(s -> s.getParentSection() != null)
                .collect(Collectors.groupingBy(s -> s.getParentSection().getId(), Collectors.counting()));

        for (ReportTemplateSection section : sections) {
            SectionType type = section.getSectionType();
            if (type == null) continue;

            long childCount = childCountByParent.getOrDefault(section.getId(), 0L);

            switch (type) {
                case GROUP -> {
                    // must contain at least one child section
                    if (childCount == 0) {
                        result.addError(new ValidationErrorDto(
                                "GROUP_HAS_NO_CHILDREN",
                                "The '" + section.getSectionCode() + "' section must contain at least one child section.",
                                section.getSectionCode(),
                                section.getSectionCode(),
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                }
                case DETAIL -> {
                    // should not have child sections
                    if (childCount > 0) {
                        result.addError(new ValidationErrorDto(
                                "DETAIL_CONTAINS_CHILDREN",
                                "The '" + section.getSectionCode() + "' section must NOT contain child sections because it is a DETAIL (leaf).",
                                section.getSectionCode(),
                                section.getSectionCode(),
                                ReportTemplateValidationSeverity.ERROR
                        ));
                    }
                }
                case SUBTOTAL, TOTAL -> {
                    // child sections optional
                }
                default -> {
                }
            }

        }
    }
}

