package com.unionsg.xaccounting.dto.reports;

/**
 * Designer operations always respond with the complete tree.
 */
public record ReportTemplateSectionDesignerResponseDto(
        Long reportTemplateId,
        ReportTemplateSectionTreeDto root
) {
}

