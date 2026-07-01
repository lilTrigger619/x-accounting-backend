package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.ProfitLossSectionType;

import java.math.BigDecimal;
import java.util.List;

public record ProfitLossSectionDto(
        String title,

        ProfitLossSectionType type,

        List<ProfitLossAccountDto> accounts,

        BigDecimal amount
) {
}
