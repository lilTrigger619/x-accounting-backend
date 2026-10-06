package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.AlertDto;
import com.unionsg.xaccounting.dto.analytics.AnalyticsFilter;
import com.unionsg.xaccounting.dto.analytics.Breakdown;
import com.unionsg.xaccounting.dto.analytics.BreakdownRow;
import com.unionsg.xaccounting.dto.analytics.CompareMode;
import com.unionsg.xaccounting.dto.analytics.DrillAccountsResponse;
import com.unionsg.xaccounting.dto.analytics.DrillLinesResponse;
import com.unionsg.xaccounting.dto.analytics.ExecutiveDashboardResponse;
import com.unionsg.xaccounting.dto.analytics.MetricDto;
import com.unionsg.xaccounting.dto.analytics.RevenueAnalyticsResponse;
import com.unionsg.xaccounting.dto.analytics.UpdateAlertSettingRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportInternalDTO;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.service.banking.BaseCurrencyService;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.reports.ProfitAndLossService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the BI services against the real database (same Postgres requirement as
 * {@code XaccountingApplicationTests}). Figures are compared before and after posting test
 * journals, so existing data does not matter. Each test rolls back.
 */
@SpringBootTest
@Transactional
class AnalyticsIntegrationTest {

    @Autowired private ExecutiveDashboardService executive;
    @Autowired private RevenueAnalyticsService revenue;
    @Autowired private AnalyticsDrillService drill;
    @Autowired private AlertSettingsService alertSettings;
    @Autowired private JournalService journalService;
    @Autowired private BaseCurrencyService baseCurrencyService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private ProfitAndLossService profitAndLossService;

    private String base;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        base = baseCurrencyService.resolve();
    }

    @Test
    void kpisMoveByExactlyWhatWasPostedAndAReversalCancelsOut() {
        ExecutiveDashboardResponse before = executive.build(todayOnly());

        post("1020", "4000", "500");   // cash sale
        post("5000", "1020", "200");   // operating expense paid
        post("6000", "1020", "100");   // cost of sales paid
        Long mistake = post("1020", "4000", "300");
        journalService.reverse(mistake, "Entered twice");

        ExecutiveDashboardResponse after = executive.build(todayOnly());

        assertThat(delta(before, after, "revenue")).isEqualByComparingTo("500");
        assertThat(delta(before, after, "grossProfit")).isEqualByComparingTo("400");
        assertThat(delta(before, after, "operatingExpenses")).isEqualByComparingTo("200");
        assertThat(delta(before, after, "netProfit")).isEqualByComparingTo("200");
        assertThat(delta(before, after, "cash")).isEqualByComparingTo("200");
        assertThat(delta(before, after, "currentAssets")).isEqualByComparingTo("200");
        assertThat(after.scope().comparison()).isNull();
    }

    @Test
    void netProfitMatchesTheProfitAndLossReport() {
        post("1020", "4000", "750");
        Long reversed = post("5000", "1020", "120");
        journalService.reverse(reversed, "Wrong account");
        LocalDate from = today.withDayOfMonth(1);

        AnalyticsFilter f = new AnalyticsFilter();
        f.setFrom(from);
        f.setTo(today);
        f.setCompare(CompareMode.NONE);
        MetricDto net = metric(executive.build(f), "netProfit");
        ProfitLossReportInternalDTO pl = profitAndLossService.generateReport(from, today);

        assertThat(net.current()).isEqualByComparingTo(pl.totalRevenue().subtract(pl.totalExpenses()));
    }

    @Test
    void comparisonFiguresUseThePreviousPeriod() {
        LocalDate yesterday = today.minusDays(1);
        postOn(yesterday, "1020", "4000", "100");
        AnalyticsFilter f = todayOnly();
        f.setCompare(CompareMode.PREVIOUS_PERIOD);
        ExecutiveDashboardResponse before = executive.build(f);

        postOn(today, "1020", "4000", "40");
        postOn(yesterday, "1020", "4000", "60");
        ExecutiveDashboardResponse after = executive.build(f);

        MetricDto b = metric(before, "revenue");
        MetricDto a = metric(after, "revenue");
        assertThat(after.scope().comparison().from()).isEqualTo(yesterday);
        assertThat(a.current().subtract(b.current())).isEqualByComparingTo("40");
        assertThat(a.previous().subtract(b.previous())).isEqualByComparingTo("60");
        assertThat(a.change()).isEqualByComparingTo(a.current().subtract(a.previous()));
        assertThat(a.explanation()).isNotBlank();
        assertThat(after.trends()).extracting(t -> t.key()).contains("revenue", "expenses", "grossProfit", "netProfit", "cash");
    }

    @Test
    void revenueSplitsAddBackToTheLedgerTotal() {
        RevenueAnalyticsResponse before = revenue.build(todayOnly());
        post("1020", "4000", "320");
        RevenueAnalyticsResponse after = revenue.build(todayOnly());

        assertThat(after.total().current().subtract(before.total().current())).isEqualByComparingTo("320");
        BigDecimal total = after.total().current();
        for (Breakdown b : after.breakdowns()) {
            if (!b.supported() || b.key().equals("product") || b.key().equals("service")) continue;
            assertThat(sum(b)).as(b.key()).isEqualByComparingTo(total);
        }
        // Products and services split the same revenue between them.
        assertThat(sum(breakdown(after, "product")).add(sum(breakdown(after, "service")))).isEqualByComparingTo(total);
        Breakdown customers = breakdown(after, "customer");
        assertThat(customers.rows()).extracting(BreakdownRow::label).contains(RevenueAnalyticsService.NOT_FROM_INVOICE);
        assertThat(after.breakdowns()).filteredOn(b -> b.key().equals("branch")).allMatch(b -> !b.supported());
    }

    @Test
    void customerFilterLeavesOutManualRevenueAndSaysSo() {
        post("1020", "4000", "999");
        AnalyticsFilter f = todayOnly();
        f.setCustomerIds(List.of(-1L));
        RevenueAnalyticsResponse r = revenue.build(f);

        assertThat(r.total().current()).isEqualByComparingTo("0");
        assertThat(r.scope().appliedFilters()).contains("Customer");
        assertThat(r.notes()).anyMatch(n -> n.contains("manual journals"));
    }

    @Test
    void executiveDashboardListsFiltersItCannotApply() {
        AnalyticsFilter f = todayOnly();
        f.setCustomerIds(List.of(1L));
        f.setDepartmentId(1L);
        ExecutiveDashboardResponse r = executive.build(f);
        assertThat(r.scope().ignoredFilters()).extracting(x -> x.filter()).contains("Customer", "Department");
    }

    @Test
    void drillDownReachesTheJournalLine() {
        Long journalId = post("1020", "4000", "275");
        Long revenueAccount = accountRepository.findByAccountId("4000").orElseThrow().getId();

        DrillAccountsResponse accounts = drill.accounts(List.of("OPERATING_REVENUE"), null, today, today, "PERIOD", "Revenue");
        assertThat(accounts.categories()).flatExtracting(DrillAccountsResponse.Category::accounts)
                .extracting(DrillAccountsResponse.Account::id).contains(revenueAccount);

        DrillLinesResponse lines = drill.lines(revenueAccount, today, today, "PERIOD", 0, 200);
        assertThat(lines.lines()).anySatisfy(l -> {
            assertThat(l.journalId()).isEqualTo(journalId);
            assertThat(l.amount()).isEqualByComparingTo("275");
        });
    }

    @Test
    void alertThresholdsCanBeChangedAndAreValidated() {
        alertSettings.update("REVENUE_DECLINE", new UpdateAlertSettingRequest(true, new BigDecimal("25"), null));
        assertThat(alertSettings.effective(AlertDefinition.REVENUE_DECLINE).threshold()).isEqualByComparingTo("25");

        assertThatThrownBy(() -> alertSettings.update("REVENUE_DECLINE", new UpdateAlertSettingRequest(null, BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> alertSettings.update("NOPE", new UpdateAlertSettingRequest(true, null, null)))
                .isInstanceOf(BusinessException.class);

        alertSettings.update("LOW_CASH", new UpdateAlertSettingRequest(false, null, null));
        AlertDto lowCash = executive.build(todayOnly()).alerts().stream()
                .filter(a -> a.key().equals("LOW_CASH")).findFirst().orElseThrow();
        assertThat(lowCash.status()).isEqualTo("DISABLED");
    }

    private static Breakdown breakdown(RevenueAnalyticsResponse r, String key) {
        return r.breakdowns().stream().filter(b -> b.key().equals(key)).findFirst().orElseThrow();
    }

    private static BigDecimal sum(Breakdown b) {
        return b.rows().stream().map(BreakdownRow::current).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private AnalyticsFilter todayOnly() {
        AnalyticsFilter f = new AnalyticsFilter();
        f.setFrom(today);
        f.setTo(today);
        f.setCompare(CompareMode.NONE);
        return f;
    }

    private static MetricDto metric(ExecutiveDashboardResponse r, String key) {
        return r.kpis().stream().filter(k -> k.key().equals(key)).findFirst().orElseThrow();
    }

    private static BigDecimal delta(ExecutiveDashboardResponse before, ExecutiveDashboardResponse after, String key) {
        return metric(after, key).current().subtract(metric(before, key).current());
    }

    private Long post(String debitCode, String creditCode, String amount) {
        return postOn(today, debitCode, creditCode, amount);
    }

    private Long postOn(LocalDate date, String debitCode, String creditCode, String amount) {
        BigDecimal value = new BigDecimal(amount);
        var created = journalService.create(CreateJournalRequest.builder()
                .journalDate(date)
                .description("Analytics test")
                .journalType(JournalType.ADJUSTMENT)
                .currencyCode(base)
                .lines(List.of(
                        CreateJournalLineRequest.builder().accountId(Long.valueOf(debitCode))
                                .debitAmount(value).creditAmount(BigDecimal.ZERO).build(),
                        CreateJournalLineRequest.builder().accountId(Long.valueOf(creditCode))
                                .debitAmount(BigDecimal.ZERO).creditAmount(value).build()))
                .build());
        journalService.post(created.getId());
        return created.getId();
    }
}
