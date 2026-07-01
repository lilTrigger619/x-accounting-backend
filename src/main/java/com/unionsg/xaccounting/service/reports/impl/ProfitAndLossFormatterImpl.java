package com.unionsg.xaccounting.service.reports.impl;
//
//import com.yourcompany.accounting.chart.enums.AccountType;
//import com.yourcompany.accounting.report.dto.*;
//        import com.yourcompany.accounting.report.service.ProfitAndLossService;
//import com.yourcompany.accounting.report.service.formatter.ProfitAndLossFormatter;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportInternalDTO;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportResponseDTO;
import com.unionsg.xaccounting.dto.reports.ProfitLossSectionDto;
import com.unionsg.xaccounting.enums.ProfitLossSectionType;
import com.unionsg.xaccounting.service.reports.ProfitAndLossFormatter;
import com.unionsg.xaccounting.service.reports.ProfitAndLossService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfitAndLossFormatterImpl implements ProfitAndLossFormatter {

    private final ProfitAndLossService profitAndLossService;

    @Override
    public ProfitLossReportResponseDTO format(LocalDate fromDate, LocalDate toDate) {

//        ProfitLossReportResponseDTO raw = profitAndLossService.generateReport(fromDate, toDate);
        ProfitLossReportInternalDTO raw = profitAndLossService.generateReport(fromDate, toDate);

        // -----------------------------
        // 1. Revenue Section
        // -----------------------------
        ProfitLossSectionDto revenueSection = new ProfitLossSectionDto(
                "Revenue",
                ProfitLossSectionType.SECTION,
                raw.revenueAccounts(),
                raw.totalRevenue()
        );

        // -----------------------------
        // 2. Cost of Sales (optional extension point)
        // For now we treat expenses as single block
        // -----------------------------
        ProfitLossSectionDto expensesSection = new ProfitLossSectionDto(
                "Expenses",
                ProfitLossSectionType.SECTION,
                raw.expenseAccounts(),
                raw.totalExpenses()
        );

        // -----------------------------
        // 3. Gross Profit
        // -----------------------------
        BigDecimal grossProfit = raw.totalRevenue()
                .subtract(raw.totalExpenses());

        ProfitLossSectionDto grossProfitSection = new ProfitLossSectionDto(
                "Gross Profit",
                ProfitLossSectionType.SUBTOTAL,
                List.of(),
                grossProfit
        );

        // -----------------------------
        // 4. Net Profit / Loss
        // -----------------------------
        BigDecimal netProfit = grossProfit.max(BigDecimal.ZERO);
        BigDecimal netLoss = grossProfit.signum() < 0
                ? grossProfit.abs()
                : BigDecimal.ZERO;

        ProfitLossSectionDto netSection = new ProfitLossSectionDto(
                grossProfit.signum() >= 0 ? "Net Profit" : "Net Loss",
                ProfitLossSectionType.TOTAL,
                List.of(),
                grossProfit
        );

        // -----------------------------
        // Final Response
        // -----------------------------
        return new ProfitLossReportResponseDTO(
                fromDate,
                toDate,
                raw.revenueAccounts(),
                raw.expenseAccounts(),
                List.of(
                        revenueSection,
                        expensesSection,
                        grossProfitSection,
                        netSection
                ),
                raw.totalRevenue(),
                raw.totalExpenses(),
                grossProfit,
                netProfit,
                netLoss
        );
    }
}