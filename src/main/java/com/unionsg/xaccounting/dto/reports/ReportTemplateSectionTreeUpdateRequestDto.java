package com.unionsg.xaccounting.dto.reports;

/**
 * Base optimistic-locking request for designer operations.
 */
public record ReportTemplateSectionTreeUpdateRequestDto(
        Long sectionId,
        Long version
) {}

