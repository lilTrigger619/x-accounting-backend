package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.SectionType;

public record ReportTemplateSectionResponseDto(
        Long id,
        Long reportTemplateId,
        Long parentSectionId,
        String sectionCode,
        String title,
        Integer displayOrder,
        SectionType sectionType,
        String formula,
        boolean visible,
        boolean expandedByDefault
) {
}

