package com.unionsg.xaccounting.service.dashboard;

import com.unionsg.xaccounting.dto.dashboard.AccountingHealthDto;
import com.unionsg.xaccounting.dto.dashboard.DashboardResponseDTO;
import com.unionsg.xaccounting.dto.dashboard.MonthlyFinancialPointDto;
import com.unionsg.xaccounting.dto.dashboard.RecentTransactionDto;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportInternalDTO;
import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.reports.CurrentFiscalPeriodResolver;
import com.unionsg.xaccounting.service.reports.ProfitAndLossService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unionsg.xaccounting.service.analytics.AccountClass;
import com.unionsg.xaccounting.service.analytics.LedgerData;
import com.unionsg.xaccounting.service.analytics.LedgerDataService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final int TREND_MONTHS = 12;
    private static final int RECENT_TRANSACTION_LIMIT = 8;

    private final ProfitAndLossService profitAndLossService;
    private final CurrentFiscalPeriodResolver currentFiscalPeriodResolver;
    private final FinancialYearRepository financialYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalService journalService;

    private final LedgerDataService ledgerDataService;

    @Override
    public DashboardResponseDTO getSummary(LocalDate asOfDate) {

        LocalDate today = asOfDate != null ? asOfDate : LocalDate.now();

        // Same account classes as the BI executive dashboard, so the header, this dashboard and
        // BI never disagree on what counts as cash, receivables or payables.
        LedgerData ledger = ledgerDataService.load(today, today);
        BigDecimal cashBalance = ledger.balanceAt(List.of(AccountClass.CASH_BANK), today);
        BigDecimal accountsReceivable = ledger.balanceAt(List.of(AccountClass.RECEIVABLE), today);
        BigDecimal accountsPayable = ledger.balanceAt(List.of(AccountClass.PAYABLE), today);

        LocalDate ytdStart = currentFiscalPeriodResolver.resolveYearStart(today);
        ProfitLossReportInternalDTO ytd = profitAndLossService.generateReport(ytdStart, today);
        BigDecimal netProfitYtd = ytd.totalRevenue().subtract(ytd.totalExpenses());

        List<MonthlyFinancialPointDto> monthlyTrend = buildMonthlyTrend(today);
        List<RecentTransactionDto> recentTransactions = buildRecentTransactions();
        AccountingHealthDto accountingHealth = buildAccountingHealth();

        return new DashboardResponseDTO(
                today,
                cashBalance, accountsReceivable, accountsPayable,
                ytdStart, netProfitYtd, ytd.totalRevenue(), ytd.totalExpenses(),
                monthlyTrend, recentTransactions, accountingHealth
        );
    }

    private List<MonthlyFinancialPointDto> buildMonthlyTrend(LocalDate today) {
        List<MonthlyFinancialPointDto> trend = new ArrayList<>();

        LocalDate cursor = today.withDayOfMonth(1).minusMonths(TREND_MONTHS - 1L);
        for (int i = 0; i < TREND_MONTHS; i++) {
            LocalDate monthStart = cursor;
            LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
            if (monthEnd.isAfter(today)) {
                monthEnd = today;
            }

            ProfitLossReportInternalDTO report = profitAndLossService.generateReport(monthStart, monthEnd);
            BigDecimal net = report.totalRevenue().subtract(report.totalExpenses());

            trend.add(new MonthlyFinancialPointDto(
                    monthStart.format(MONTH_LABEL), monthStart, report.totalRevenue(), report.totalExpenses(), net
            ));

            cursor = cursor.plusMonths(1);
        }

        return trend;
    }

    private List<RecentTransactionDto> buildRecentTransactions() {
        List<JournalResponse> journals = journalService.getAll(
                null, JournalStatus.POSTED, null, null, null, null,
                PageRequest.of(0, RECENT_TRANSACTION_LIMIT, Sort.by(Sort.Direction.DESC, "journalDate", "id"))
        ).getContent();

        return journals.stream()
                .map(j -> new RecentTransactionDto(
                        j.getId(), j.getJournalNumber(), j.getJournalDate(), j.getDescription(),
                        j.getReference(), j.getJournalType() != null ? j.getJournalType().name() : null,
                        j.getSourceModule(), j.getTotalDebit()
                ))
                .collect(Collectors.toList());
    }

    private AccountingHealthDto buildAccountingHealth() {
        FinancialYear currentFy = financialYearRepository.findByIsCurrentTrue().orElse(null);
        long lockedPeriods = accountingPeriodRepository.countByStatus(AccountingPeriodStatus.LOCKED);
        long draftJournals = journalEntryRepository.countByStatus(JournalStatus.DRAFT);

        return new AccountingHealthDto(
                currentFy != null ? currentFy.getName() : null,
                currentFy != null ? currentFy.getStatus().name() : null,
                lockedPeriods,
                draftJournals,
                currentFy != null && currentFy.isHasOpeningBalance()
        );
    }
}
