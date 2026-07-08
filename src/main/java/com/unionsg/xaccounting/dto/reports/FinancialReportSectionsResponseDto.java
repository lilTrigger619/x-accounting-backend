package com.unionsg.xaccounting.dto.reports;

import java.time.LocalDate;
import java.util.List;

public record FinancialReportSectionsResponseDto(
        String reportName,
        LocalDate fromDate,
        LocalDate toDate,
        List<FinancialReportSectionsResponseNodeDto> sections
) {
}

