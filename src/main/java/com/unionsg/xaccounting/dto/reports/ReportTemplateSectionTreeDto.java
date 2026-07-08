package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.SectionType;

import java.util.List;

/**
 * Response shape for drag-and-drop designer.
 * Always returns the full hierarchy tree.
 */
public record ReportTemplateSectionTreeDto(
        Long id,
        Long reportTemplateId,
        Long parentSectionId,
        String sectionCode,
        String title,
        Integer displayOrder,
        SectionType sectionType,
        String formula,
        boolean visible,
        boolean expandedByDefault,
        boolean collapsed,
        List<ReportTemplateSectionTreeDto> children
) {
}

