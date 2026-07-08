package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.SectionType;

import java.math.BigDecimal;
import java.util.List;

public record FinancialReportSectionsResponseNodeDto(
        String title,
        SectionType type,
        BigDecimal amount,
        List<FinancialReportSectionsResponseNodeDto> children
) {
}

