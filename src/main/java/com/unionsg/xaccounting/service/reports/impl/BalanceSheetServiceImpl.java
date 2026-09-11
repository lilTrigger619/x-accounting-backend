package com.unionsg.xaccounting.service.reports.impl;

import com.unionsg.xaccounting.dto.reports.BalanceSheetAccountDto;
import com.unionsg.xaccounting.dto.reports.BalanceSheetResponseDTO;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportInternalDTO;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.projection.ProfitLossAccountProjection;
import com.unionsg.xaccounting.repository.reports.LedgerAsOfBalanceRepository;
import com.unionsg.xaccounting.service.reports.BalanceSheetService;
import com.unionsg.xaccounting.service.reports.CurrentFiscalPeriodResolver;
import com.unionsg.xaccounting.service.reports.ProfitAndLossService;
import com.unionsg.xaccounting.utils.CalculateBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BalanceSheetServiceImpl implements BalanceSheetService {

    private final LedgerAsOfBalanceRepository ledgerAsOfBalanceRepository;
    private final CurrentFiscalPeriodResolver currentFiscalPeriodResolver;
    private final ProfitAndLossService profitAndLossService;

    @Override
    public BalanceSheetResponseDTO generateReport(LocalDate asOfDate) {

        if (asOfDate == null) {
            throw new BadRequestException("As-of date is required");
        }

        List<ProfitLossAccountProjection> rows = ledgerAsOfBalanceRepository.findAsOfBalances(
                asOfDate,
                List.of(AccountType.ASSET, AccountType.LIABILITY, AccountType.EQUITY)
        );

        List<BalanceSheetAccountDto> assets = new ArrayList<>();
        List<BalanceSheetAccountDto> liabilities = new ArrayList<>();
        List<BalanceSheetAccountDto> equity = new ArrayList<>();

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        for (ProfitLossAccountProjection row : rows) {

            BigDecimal balance = CalculateBalance.calculateBalance(
                    row.getTotalDebit(), row.getTotalCredit(), row.getNormalBalance()
            );

            if (balance.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            BalanceSheetAccountDto dto = new BalanceSheetAccountDto(
                    row.getAccountId(), row.getAccountCode(), row.getAccountName(), balance
            );

            switch (row.getAccountType()) {
                case ASSET -> {
                    assets.add(dto);
                    totalAssets = totalAssets.add(balance);
                }
                case LIABILITY -> {
                    liabilities.add(dto);
                    totalLiabilities = totalLiabilities.add(balance);
                }
                case EQUITY -> {
                    equity.add(dto);
                    totalEquity = totalEquity.add(balance);
                }
                default -> { /* INCOME/EXPENSE never requested for this report */ }
            }
        }

        BigDecimal currentYearEarnings = computeCurrentYearEarnings(asOfDate);
        BigDecimal totalEquityAndCurrentEarnings = totalEquity.add(currentYearEarnings);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquityAndCurrentEarnings);
        BigDecimal balanceCheck = totalAssets.subtract(totalLiabilitiesAndEquity);

        return new BalanceSheetResponseDTO(
                asOfDate,
                assets, totalAssets,
                liabilities, totalLiabilities,
                equity, totalEquity,
                currentYearEarnings, totalEquityAndCurrentEarnings,
                totalLiabilitiesAndEquity,
                balanceCheck
        );
    }

    /**
     * Net income earned since the books were last closed to Retained Earnings, so the balance
     * sheet balances for any date that falls before the next Year-End Closing. Uses the active
     * Financial Year's start date when one is configured; otherwise falls back to calendar
     * year-to-date so the report still produces a sensible number before Financial Years are
     * set up at all.
     */
    private BigDecimal computeCurrentYearEarnings(LocalDate asOfDate) {
        LocalDate periodStart = currentFiscalPeriodResolver.resolveYearStart(asOfDate);

        if (periodStart.isAfter(asOfDate)) {
            return BigDecimal.ZERO;
        }

        ProfitLossReportInternalDTO ytd = profitAndLossService.generateReport(periodStart, asOfDate);
        return ytd.totalRevenue().subtract(ytd.totalExpenses());
    }
}
