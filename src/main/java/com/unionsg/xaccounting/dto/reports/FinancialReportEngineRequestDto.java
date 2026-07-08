package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record FinancialReportEngineRequestDto(
        @NotBlank String reportCode,
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {
}

