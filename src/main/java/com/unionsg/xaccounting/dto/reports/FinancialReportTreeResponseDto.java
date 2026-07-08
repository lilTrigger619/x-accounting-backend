package com.unionsg.xaccounting.dto.reports;

import java.time.LocalDate;

public record FinancialReportTreeResponseDto(
        String reportCode,
        LocalDate from,
        LocalDate to,
        FinancialReportTreeNodeDto root
) {
}

