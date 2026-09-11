package com.unionsg.xaccounting.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponseDTO(

        LocalDate asOfDate,

        BigDecimal cashBalance,
        BigDecimal accountsReceivable,
        BigDecimal accountsPayable,

        LocalDate ytdStart,
        BigDecimal netProfitYtd,
        BigDecimal revenueYtd,
        BigDecimal expenseYtd,

        List<MonthlyFinancialPointDto> monthlyTrend,
        List<RecentTransactionDto> recentTransactions,
        AccountingHealthDto accountingHealth

) {
}
