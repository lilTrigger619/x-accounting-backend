package com.unionsg.xaccounting.dto.reports;

import java.util.List;

public record ReportSectionAccountListResponseDto(
        List<ReportSectionAccountResponseDto> content
) {
}

