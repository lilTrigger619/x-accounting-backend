package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;

public record ReportSectionAccountBulkItemDto(
        @NotNull Long accountId,
        @NotNull Integer displayOrder
) {
}

