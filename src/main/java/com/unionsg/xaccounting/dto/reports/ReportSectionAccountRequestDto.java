package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;

public record ReportSectionAccountRequestDto(
        @NotNull Long reportSectionId,
        @NotNull Long accountId,
        @NotNull Integer displayOrder
) {
}

