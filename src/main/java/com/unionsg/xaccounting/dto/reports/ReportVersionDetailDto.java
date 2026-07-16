package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.enums.SectionType;

import java.time.LocalDateTime;
import java.util.List;

public record ReportVersionDetailDto(
        Long templateId,
        String templateCode,
        String templateName,
        String description,
        String category,
        ReportTemplateStatus status,
        Integer version,
        boolean isSystemTemplate,
        String publishedBy,
        LocalDateTime publishedAt,
        List<SectionDto> sections,
        List<FormulaDto> formulas,
        List<AccountAssignmentDto> accountAssignments
) {

    public record SectionDto(
            Long id,
            String sectionCode,
            String title,
            Integer displayOrder,
            SectionType sectionType,
            String formula,
            boolean visible,
            boolean expandedByDefault,
            String parentSectionCode
    ) {
    }

    public record FormulaDto(
            String sectionCode,
            String formula
    ) {
    }

    public record AccountAssignmentDto(
            String sectionCode,
            Long accountId,
            Integer displayOrder
    ) {
    }
}

