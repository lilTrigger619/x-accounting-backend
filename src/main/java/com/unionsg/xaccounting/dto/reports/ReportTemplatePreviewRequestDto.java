package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReportTemplatePreviewRequestDto(
        @NotNull LocalDate fromDate,
        @NotNull LocalDate toDate
) {
}

