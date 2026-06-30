package com.unionsg.xaccounting.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProfitLossReportResponseDTO
        (

        LocalDate fromDate,

        LocalDate toDate,

        List<ProfitLossAccountDto> revenueAccounts,

        List<ProfitLossAccountDto> expenseAccounts,

        BigDecimal totalRevenue,

        BigDecimal totalExpenses,

        BigDecimal netProfit,

        BigDecimal netLoss

) {
}