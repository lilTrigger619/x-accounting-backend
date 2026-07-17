package com.unionsg.xaccounting.service.reports.template.validation.impl;

import com.unionsg.xaccounting.dto.reports.ValidationErrorDto;
import com.unionsg.xaccounting.dto.reports.ValidationWarningDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.enums.ReportTemplateValidationSeverity;
import com.unionsg.xaccounting.service.reports.template.validation.StructureValidator;
import com.unionsg.xaccounting.service.reports.template.validation.ValidationResult;
import com.unionsg.xaccounting.repository.reports.ReportTemplateSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StructureValidatorImpl implements StructureValidator {

    private final ReportTemplateSectionRepository sectionRepository;

    @Override
    @Transactional(readOnly = true)
    public void validate(ReportTemplate template, ValidationResult result) {
        List<ReportTemplateSection> sections = sectionRepository.findByReportTemplateId(template.getId());

        if (sections == null || sections.isEmpty()) {
            result.addError(new ValidationErrorDto(
                    "STRUCTURE_EMPTY",
                    "Template must contain at least one section.",
                    null,
                    "template",
                    ReportTemplateValidationSeverity.ERROR
            ));
            return;
        }

        // Duplicate section codes
        Map<String, List<ReportTemplateSection>> byCode = new HashMap<>();
        for (ReportTemplateSection s : sections) {
            String code = s.getSectionCode();
            byCode.computeIfAbsent(code, k -> new ArrayList<>()).add(s);
        }
        for (var e : byCode.entrySet()) {
            if (e.getValue().size() > 1) {
                result.addError(new ValidationErrorDto(
                        "STRUCTURE_DUP_SECTION_CODE",
                        "Duplicate sectionCode in template: '" + e.getKey() + "'.",
                        "" + e.getKey(),
                        "template",
                        ReportTemplateValidationSeverity.ERROR
                ));
            }
        }

        // Parent existence
        Set<Long> sectionIds = new HashSet<>();
        for (ReportTemplateSection s : sections) sectionIds.add(s.getId());

        for (ReportTemplateSection s : sections) {
            if (s.getParentSection() == null) continue;
            if (!sectionIds.contains(s.getParentSection().getId())) {
                result.addError(new ValidationErrorDto(
                        "STRUCTURE_MISSING_PARENT",
                        "Parent section reference not found for section '" + s.getSectionCode() + "'.",
                        s.getSectionCode(),
                        s.getSectionCode(),
                        ReportTemplateValidationSeverity.ERROR
                ));
            }
        }

        // Sibling displayOrder duplicates
        Map<Long, Map<Integer, Integer>> seenOrders = new HashMap<>();
        for (ReportTemplateSection s : sections) {
            Long parentId = s.getParentSection() == null ? -1L : s.getParentSection().getId();
            seenOrders.putIfAbsent(parentId, new HashMap<>());
            Map<Integer, Integer> m = seenOrders.get(parentId);
            m.put(s.getDisplayOrder(), m.getOrDefault(s.getDisplayOrder(), 0) + 1);
        }

        for (var parentEntry : seenOrders.entrySet()) {
            for (var orderEntry : parentEntry.getValue().entrySet()) {
                if (orderEntry.getValue() > 1) {
                    result.addError(new ValidationErrorDto(
                            "STRUCTURE_DISPLAY_ORDER_CONFLICT",
                            "Duplicate displayOrder among sibling sections (parent=" + parentEntry.getKey() + ", order=" + orderEntry.getKey() + ").",
                            null,
                            "displayOrder",
                            ReportTemplateValidationSeverity.ERROR
                    ));
                }
            }
        }

        // Warnings: hidden / unused section (best-effort based on visible flags and children count)
        Map<Long, Long> childrenCountByParentId = new HashMap<>();
        for (ReportTemplateSection s : sections) {
            if (s.getParentSection() == null) continue;
            Long pid = s.getParentSection().getId();
            childrenCountByParentId.put(pid, childrenCountByParentId.getOrDefault(pid, 0L) + 1);
        }

        for (ReportTemplateSection s : sections) {
            if (!s.isVisible()) {
                result.addWarning(new ValidationWarningDto(
                        "HIDDEN_SECTION",
                        "Section is hidden.",
                        s.getSectionCode(),
                        s.getSectionCode(),
                        ReportTemplateValidationSeverity.WARNING
                ));
            }

            long children = childrenCountByParentId.getOrDefault(s.getId(), 0L);
            if (children == 0) {
                result.addWarning(new ValidationWarningDto(
                        "UNUSED_SECTION",
                        "Section contains no child sections.",
                        s.getSectionCode(),
                        s.getSectionCode(),
                        ReportTemplateValidationSeverity.WARNING
                ));
            }

            if (s.getParentSection() != null && childrenCountByParentId.getOrDefault(s.getParentSection().getId(), 0L) == 1L) {
                // Section has only one child (per your warning definition)
                result.addWarning(new ValidationWarningDto(
                        "SECTION_HAS_ONLY_ONE_CHILD",
                        "A parent section has only one child section.",
                        s.getSectionCode(),
                        s.getSectionCode(),
                        ReportTemplateValidationSeverity.WARNING
                ));
            }

            if (s.getTitle() == null || s.getTitle().trim().isEmpty()) {
                result.addWarning(new ValidationWarningDto(
                        "EMPTY_DESCRIPTION",
                        "Section description/title is empty.",
                        s.getSectionCode(),
                        s.getSectionCode(),
                        ReportTemplateValidationSeverity.WARNING
                ));
            }
        }
    }
}

