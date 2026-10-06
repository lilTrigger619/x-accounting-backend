package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.AlertDto;
import com.unionsg.xaccounting.dto.analytics.AnalyticsFilter;
import com.unionsg.xaccounting.dto.analytics.DrillTarget;
import com.unionsg.xaccounting.dto.analytics.ExecutiveDashboardResponse;
import com.unionsg.xaccounting.dto.analytics.MetricDto;
import com.unionsg.xaccounting.dto.analytics.Period;
import com.unionsg.xaccounting.dto.analytics.SeriesPoint;
import com.unionsg.xaccounting.dto.analytics.TrendDto;
import com.unionsg.xaccounting.repository.analytics.AnalyticsLedgerRepository;
import com.unionsg.xaccounting.repository.analytics.AnalyticsOpenDocumentRow;
import com.unionsg.xaccounting.service.analytics.Metrics.Polarity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import static com.unionsg.xaccounting.service.analytics.AccountClass.*;
import static com.unionsg.xaccounting.service.analytics.Metrics.MONEY;
import static com.unionsg.xaccounting.service.analytics.Metrics.PERCENT;

/** "How is the business doing financially right now?" Every card uses one scope and one ledger load. */
@Service
@RequiredArgsConstructor
public class ExecutiveDashboardService {

    static final String UNSUPPORTED = "The executive dashboard is company-wide; use the Revenue, Receivables or Payables analytics to filter by this.";

    private final AnalyticsScopeService scopeService;
    private final LedgerDataService ledgerDataService;
    private final AlertSettingsService alertSettings;
    private final AnalyticsLedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public ExecutiveDashboardResponse build(AnalyticsFilter filter) {
        AnalyticsScopeService.Resolved r = scopeService.resolve(filter, Set.of(), UNSUPPORTED,
                "General ledger: posted journals, with reversed journals and their reversals both counted so they cancel out. Drafts are excluded.");
        Period p = r.period();
        Period c = r.comparison();
        String cur = r.baseCurrency();

        LedgerData data = ledgerDataService.load(r.earliest(), r.latest());
        FinancialFigures now = FinancialFigures.of(data, p.from(), p.to());
        FinancialFigures before = c == null ? null : FinancialFigures.of(data, c.from(), c.to());

        List<MetricDto> kpis = new ArrayList<>();
        kpis.add(m("revenue", "Revenue", MONEY, now.revenue(), before == null ? null : before.revenue(), Polarity.HIGHER_IS_BETTER,
                "Net credits to operating revenue accounts in the period (other income excluded).",
                period("Revenue", OPERATING_REVENUE), c, cur));
        kpis.add(m("grossProfit", "Gross Profit", MONEY, now.grossProfit(), before == null ? null : before.grossProfit(), Polarity.HIGHER_IS_BETTER,
                "Revenue minus cost of sales (accounts in the Cost of Goods Sold chart group).",
                period("Gross profit", OPERATING_REVENUE, COST_OF_SALES), c, cur));
        kpis.add(m("grossMargin", "Gross Profit Margin", PERCENT, now.grossMargin(), before == null ? null : before.grossMargin(), Polarity.HIGHER_IS_BETTER,
                "Gross profit as a percentage of revenue.",
                period("Gross profit", OPERATING_REVENUE, COST_OF_SALES), c, cur));
        kpis.add(m("operatingExpenses", "Operating Expenses", MONEY, now.operatingExpenses(), before == null ? null : before.operatingExpenses(), Polarity.LOWER_IS_BETTER,
                "Expense accounts other than cost of sales and other expenses (FX losses, write-offs, accounts categorised as other expense).",
                period("Operating expenses", OPERATING_EXPENSE), c, cur));
        kpis.add(m("netProfit", "Net Profit", MONEY, now.netProfit(), before == null ? null : before.netProfit(), Polarity.HIGHER_IS_BETTER,
                "Revenue plus other income, minus cost of sales, operating expenses and other expenses. Matches the Profit & Loss report.",
                period("Net profit", OPERATING_REVENUE, OTHER_INCOME, COST_OF_SALES, OPERATING_EXPENSE, OTHER_EXPENSE), c, cur));
        kpis.add(m("netMargin", "Net Profit Margin", PERCENT, now.netMargin(), before == null ? null : before.netMargin(), Polarity.HIGHER_IS_BETTER,
                "Net profit as a percentage of revenue.",
                period("Net profit", OPERATING_REVENUE, OTHER_INCOME, COST_OF_SALES, OPERATING_EXPENSE, OTHER_EXPENSE), c, cur));
        kpis.add(m("cash", "Cash & Bank Balance", MONEY, now.cash(), before == null ? null : before.cash(), Polarity.HIGHER_IS_BETTER,
                "Balance at the end of the period of every cash and bank account (Bank Account chart group, bank accounts' GL accounts, mapped cash and bank accounts).",
                balance("Cash & bank", CASH_BANK), c, cur));
        kpis.add(m("receivables", "Accounts Receivable", MONEY, now.receivables(), before == null ? null : before.receivables(), Polarity.NEUTRAL,
                "Balance of the receivable control accounts at the end of the period.",
                balance("Accounts receivable", RECEIVABLE), c, cur));
        kpis.add(m("payables", "Accounts Payable", MONEY, now.payables(), before == null ? null : before.payables(), Polarity.NEUTRAL,
                "Balance of the payable control accounts at the end of the period.",
                balance("Accounts payable", PAYABLE), c, cur));
        kpis.add(m("workingCapital", "Working Capital", MONEY, now.workingCapital(), before == null ? null : before.workingCapital(), Polarity.HIGHER_IS_BETTER,
                "Current assets minus current liabilities.",
                balance("Working capital", CASH_BANK, RECEIVABLE, OTHER_CURRENT_ASSET, PAYABLE, LOAN_LIABILITY, OTHER_CURRENT_LIABILITY), c, cur));
        kpis.add(m("loans", "Outstanding Loans", MONEY, now.loans(), before == null ? null : before.loans(), Polarity.LOWER_IS_BETTER,
                "Balance of the loans payable account and every borrowed loan's principal account at the end of the period.",
                balance("Outstanding loans", LOAN_LIABILITY), c, cur));
        kpis.add(m("currentAssets", "Current Assets", MONEY, now.currentAssets(), before == null ? null : before.currentAssets(), Polarity.HIGHER_IS_BETTER,
                "Cash and bank, receivables and other current assets (all assets except fixed asset categories).",
                balance("Current assets", CASH_BANK, RECEIVABLE, OTHER_CURRENT_ASSET), c, cur));
        kpis.add(m("currentLiabilities", "Current Liabilities", MONEY, now.currentLiabilities(), before == null ? null : before.currentLiabilities(), Polarity.LOWER_IS_BETTER,
                "Payables, loans payable and other liabilities, except categories marked long term.",
                balance("Current liabilities", PAYABLE, LOAN_LIABILITY, OTHER_CURRENT_LIABILITY), c, cur));

        List<TrendDto> trends = trends(r, data);
        List<AlertDto> alerts = alerts(r, now, before);

        List<String> notes = new ArrayList<>();
        long foreign = ledgerRepository.countForeignJournalsWithoutRate(r.earliest(), r.latest(), cur);
        if (foreign > 0) {
            notes.add(foreign + " posted journal(s) in this range were recorded in a currency other than " + cur
                    + " without an exchange rate, so their amounts are counted as if they were " + cur + ".");
        }
        return new ExecutiveDashboardResponse(r.scope(), kpis, trends, alerts, notes);
    }

    private List<TrendDto> trends(AnalyticsScopeService.Resolved r, LedgerData data) {
        List<Buckets.Bucket> cur = Buckets.of(r.period().from(), r.period().to(), r.scope().granularity());
        List<Buckets.Bucket> prev = r.comparison() == null ? List.of()
                : Buckets.of(r.comparison().from(), r.comparison().to(), r.scope().granularity());

        List<TrendDto> out = new ArrayList<>();
        out.add(trend("revenue", "Revenue", cur, prev,
                (s, e) -> data.activity(List.of(OPERATING_REVENUE), s, e), period("Revenue", OPERATING_REVENUE)));
        out.add(trend("expenses", "Expenses", cur, prev,
                (s, e) -> data.activity(FinancialFigures.ALL_EXPENSES, s, e),
                period("Expenses", COST_OF_SALES, OPERATING_EXPENSE, OTHER_EXPENSE)));
        out.add(trend("grossProfit", "Gross Profit", cur, prev,
                (s, e) -> data.activity(List.of(OPERATING_REVENUE), s, e).subtract(data.activity(List.of(COST_OF_SALES), s, e)),
                period("Gross profit", OPERATING_REVENUE, COST_OF_SALES)));
        out.add(trend("netProfit", "Net Profit", cur, prev,
                (s, e) -> FinancialFigures.of(data, s, e).netProfit(),
                period("Net profit", OPERATING_REVENUE, OTHER_INCOME, COST_OF_SALES, OPERATING_EXPENSE, OTHER_EXPENSE)));
        out.add(trend("cash", "Cash & Bank (closing balance)", cur, prev,
                (s, e) -> data.balanceAt(List.of(CASH_BANK), e), balance("Cash & bank", CASH_BANK)));
        return out;
    }

    static TrendDto trend(String key, String label, List<Buckets.Bucket> cur, List<Buckets.Bucket> prev,
                          BiFunction<LocalDate, LocalDate, BigDecimal> value, DrillTarget drill) {
        List<SeriesPoint> points = new ArrayList<>();
        BigDecimal last = null;
        for (int i = 0; i < cur.size(); i++) {
            Buckets.Bucket b = cur.get(i);
            BigDecimal v = Metrics.scale(value.apply(b.start(), b.end()));
            Buckets.Bucket pb = i < prev.size() ? prev.get(i) : null;
            BigDecimal pv = pb == null ? null : Metrics.scale(value.apply(pb.start(), pb.end()));
            points.add(new SeriesPoint(b.label(), b.start(), b.end(), v, pb == null ? null : pb.label(), pv,
                    last == null ? null : Metrics.growth(v, last)));
            last = v;
        }
        return new TrendDto(key, label, MONEY, points, drill);
    }

    private List<AlertDto> alerts(AnalyticsScopeService.Resolved r, FinancialFigures now, FinancialFigures before) {
        List<AlertDto> out = new ArrayList<>();
        Period p = r.period();
        String cmp = r.comparison() == null ? null : r.comparison().label();

        // Revenue decline
        out.add(evaluate(AlertDefinition.REVENUE_DECLINE, before == null ? null : negate(Metrics.growth(now.revenue(), before.revenue())),
                before == null ? "Choose a comparison period to check this." : "Revenue was zero in the comparison period.",
                v -> "Revenue " + (v.signum() >= 0 ? "fell " : "grew ") + Metrics.number(v.abs()) + "% against " + cmp + ".",
                period("Revenue", OPERATING_REVENUE), true));

        // Expense increase
        out.add(evaluate(AlertDefinition.EXPENSE_INCREASE, before == null ? null : Metrics.growth(now.totalExpenses(), before.totalExpenses()),
                before == null ? "Choose a comparison period to check this." : "There were no expenses in the comparison period.",
                v -> "Total expenses " + (v.signum() >= 0 ? "rose " : "fell ") + Metrics.number(v.abs()) + "% against " + cmp + ".",
                period("Expenses", COST_OF_SALES, OPERATING_EXPENSE, OTHER_EXPENSE), true));

        // Low cash: months of cover at the period's average monthly expense
        BigDecimal months = BigDecimal.valueOf(ChronoUnit.DAYS.between(p.from(), p.to()) + 1)
                .divide(new BigDecimal("30.4375"), 6, RoundingMode.HALF_UP);
        BigDecimal monthlyBurn = now.totalExpenses().signum() > 0
                ? now.totalExpenses().divide(months, 6, RoundingMode.HALF_UP) : null;
        BigDecimal cover = monthlyBurn == null ? null : now.cash().divide(monthlyBurn, 2, RoundingMode.HALF_UP);
        out.add(evaluate(AlertDefinition.LOW_CASH, cover, "There were no expenses in the period to measure cash cover against.",
                v -> "Cash and bank balances of " + Metrics.money(now.cash(), r.baseCurrency()) + " cover "
                        + Metrics.number(v) + " month(s) of average expenses (" + Metrics.money(monthlyBurn, r.baseCurrency()) + " a month).",
                balance("Cash & bank", CASH_BANK), false));

        // Overdue receivables share at period end
        List<AnalyticsOpenDocumentRow> invoices = ledgerRepository.findOpenInvoices();
        BigDecimal open = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        for (AnalyticsOpenDocumentRow inv : invoices) {
            BigDecimal bal = LedgerData.nz(inv.getBalance());
            open = open.add(bal);
            if (inv.getDueDate() != null && inv.getDueDate().isBefore(p.to())) overdue = overdue.add(bal);
        }
        BigDecimal overdueShare = Metrics.pct(overdue, open);
        BigDecimal finalOverdue = overdue;
        out.add(evaluate(AlertDefinition.HIGH_OVERDUE_RECEIVABLES, overdueShare, "There are no open invoices.",
                v -> Metrics.number(v) + "% of open invoice balances (" + Metrics.number(finalOverdue)
                        + ") were past due on " + p.to().format(AnalyticsScopeService.LABEL) + ". Uses today's invoice balances.",
                balance("Accounts receivable", RECEIVABLE), true));

        // Payables due in the window against cash
        AlertSettingsService.Effective payCfg = alertSettings.effective(AlertDefinition.PAYABLE_OBLIGATIONS);
        int window = payCfg.windowDays() == null ? 30 : payCfg.windowDays();
        LocalDate horizon = p.to().plusDays(window);
        BigDecimal due = BigDecimal.ZERO;
        for (AnalyticsOpenDocumentRow bill : ledgerRepository.findOpenBills()) {
            if (bill.getDueDate() == null || !bill.getDueDate().isAfter(horizon)) due = due.add(LedgerData.nz(bill.getBalance()));
        }
        BigDecimal finalDue = due;
        BigDecimal dueShare = now.cash().signum() > 0 ? Metrics.pct(due, now.cash())
                : (due.signum() > 0 ? new BigDecimal("999999") : null);
        out.add(evaluate(AlertDefinition.PAYABLE_OBLIGATIONS, dueShare, "There are no unpaid bills and no cash to compare.",
                v -> "Unpaid bills of " + Metrics.number(finalDue) + " fall due by " + horizon.format(AnalyticsScopeService.LABEL)
                        + (now.cash().signum() > 0 ? ", " + Metrics.number(v) + "% of cash and bank balances." : ", and cash and bank balances are not positive."),
                balance("Accounts payable", PAYABLE), true));

        // Budget variance: no budgets exist yet
        AlertSettingsService.Effective budgetCfg = alertSettings.effective(AlertDefinition.BUDGET_VARIANCE);
        out.add(new AlertDto(AlertDefinition.BUDGET_VARIANCE.name(), AlertDefinition.BUDGET_VARIANCE.title(),
                budgetCfg.enabled() ? "NOT_AVAILABLE" : "DISABLED", null,
                "Budgets are not set up yet. This check starts working once Budget vs Actual is built.",
                null, budgetCfg.threshold(), AlertDefinition.BUDGET_VARIANCE.unit(), null));

        // Profitability decline in margin points
        BigDecimal marginDrop = before == null || now.netMargin() == null || before.netMargin() == null ? null
                : before.netMargin().subtract(now.netMargin());
        out.add(evaluate(AlertDefinition.PROFITABILITY_DECLINE, marginDrop,
                before == null ? "Choose a comparison period to check this." : "Net margin needs revenue in both periods.",
                v -> "Net profit margin " + (v.signum() >= 0 ? "fell " : "rose ") + Metrics.number(v.abs()) + " points, from " + Metrics.number(before.netMargin())
                        + "% to " + Metrics.number(now.netMargin()) + "%.",
                period("Net profit", OPERATING_REVENUE, OTHER_INCOME, COST_OF_SALES, OPERATING_EXPENSE, OTHER_EXPENSE), true));
        return out;
    }

    /**
     * @param higherIsWorse true when the alert fires at or above the threshold (most checks);
     *                      false when it fires at or below it (cash cover).
     */
    private AlertDto evaluate(AlertDefinition def, BigDecimal value, String noDataReason,
                              java.util.function.Function<BigDecimal, String> message, DrillTarget drill, boolean higherIsWorse) {
        AlertSettingsService.Effective cfg = alertSettings.effective(def);
        if (!cfg.enabled()) {
            return new AlertDto(def.name(), def.title(), "DISABLED", null, "Switched off in BI settings.", null, cfg.threshold(), def.unit(), drill);
        }
        if (value == null) {
            return new AlertDto(def.name(), def.title(), "INSUFFICIENT_DATA", null, noDataReason, null, cfg.threshold(), def.unit(), drill);
        }
        BigDecimal t = cfg.threshold();
        boolean triggered = higherIsWorse ? value.compareTo(t) >= 0 : value.compareTo(t) < 0;
        String severity = null;
        if (triggered) {
            boolean critical = higherIsWorse ? value.compareTo(t.multiply(BigDecimal.valueOf(2))) >= 0
                    : value.compareTo(t.divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)) < 0;
            severity = critical ? "CRITICAL" : "WARNING";
        }
        return new AlertDto(def.name(), def.title(), triggered ? "TRIGGERED" : "CLEAR", severity,
                message.apply(value), Metrics.scale(value), t, def.unit(), drill);
    }

    private static BigDecimal negate(BigDecimal v) {
        return v == null ? null : v.negate();
    }

    private static MetricDto m(String key, String label, String format, BigDecimal cur, BigDecimal prev, Polarity polarity,
                               String definition, DrillTarget drill, Period c, String currency) {
        return Metrics.metric(key, label, format, cur, prev, polarity, definition, drill, c, currency);
    }

    static DrillTarget period(String label, AccountClass... classes) {
        return new DrillTarget(label, names(classes), List.of(), "PERIOD");
    }

    static DrillTarget balance(String label, AccountClass... classes) {
        return new DrillTarget(label, names(classes), List.of(), "BALANCE");
    }

    private static List<String> names(AccountClass... classes) {
        List<String> out = new ArrayList<>();
        for (AccountClass c : classes) out.add(c.name());
        return out;
    }
}
