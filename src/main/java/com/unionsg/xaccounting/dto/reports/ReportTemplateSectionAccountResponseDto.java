package com.unionsg.xaccounting.dto.reports;

public record ReportTemplateSectionAccountResponseDto(
        Long id,
        Long reportTemplateSectionId,
        Long accountId,
        Integer displayOrder
) {
}

