package com.unionsg.xaccounting.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthlyFinancialPointDto(

        String monthLabel,
        LocalDate monthStart,
        BigDecimal revenue,
        BigDecimal expense,
        BigDecimal netProfit

) {
}
