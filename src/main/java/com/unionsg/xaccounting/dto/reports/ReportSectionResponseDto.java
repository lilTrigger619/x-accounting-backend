package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.SectionType;

public record ReportSectionResponseDto(
        Long id,
        Long reportDefinitionId,
        Long parentSectionId,
        String title,
        String code,
        Integer displayOrder,
        SectionType sectionType,
        String formula,
        boolean active
) {
}

