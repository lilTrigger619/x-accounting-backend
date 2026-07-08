package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReportSectionAccountBulkRequestDto(
        @NotNull Long reportSectionId,
        @NotNull @Valid List<ReportSectionAccountBulkItemDto> accounts
) {
}

