package com.unionsg.xaccounting.dto.reports;

public record ReportSectionAccountResponseDto(
        Long id,
        Long reportSectionId,
        Long accountId,
        Integer displayOrder
) {
}

