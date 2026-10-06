package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.AnalyticsContextResponse;
import com.unionsg.xaccounting.dto.analytics.FilterSupport;
import com.unionsg.xaccounting.dto.analytics.Period;
import com.unionsg.xaccounting.dto.config.ConfigDto;
import com.unionsg.xaccounting.dto.config.ConfigItemDto;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.service.ConfigService;
import com.unionsg.xaccounting.service.banking.BaseCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** What the BI filter bar needs: base currency, current year and period, and which filters work. */
@Service
@RequiredArgsConstructor
public class AnalyticsContextService {

    private final BaseCurrencyService baseCurrencyService;
    private final ConfigService configService;
    private final FinancialYearRepository financialYearRepository;
    private final AccountingPeriodRepository accountingPeriodRepository;
    private final AccountClassifier classifier;

    @Transactional(readOnly = true)
    public AnalyticsContextResponse context() {
        LocalDate today = LocalDate.now();
        String base = baseCurrencyService.resolve();

        Period fy = financialYearRepository.findByIsCurrentTrue()
                .or(() -> financialYearRepository.findByDateInRange(today))
                .map(y -> new Period(y.getStartDate(), y.getEndDate(), y.getName()))
                .orElse(new Period(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()), "Calendar year " + today.getYear()));
        Period period = accountingPeriodRepository.findByDateInRange(today)
                .map(ap -> new Period(ap.getStartDate(), ap.getEndDate(), ap.getName()))
                .orElse(new Period(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()),
                        today.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + today.getYear()));

        Set<String> currencies = new LinkedHashSet<>();
        currencies.add(base);
        try {
            ConfigDto cfg = configService.getConfigByKey("currencies");
            if (cfg != null && cfg.getItems() != null) {
                for (ConfigItemDto item : cfg.getItems()) {
                    if (item.getCode() != null && !item.getCode().isBlank()) currencies.add(item.getCode().trim().toUpperCase(Locale.ROOT));
                }
            }
        } catch (RuntimeException ignored) {
            // No currencies configured yet.
        }

        List<FilterSupport> filters = List.of(
                new FilterSupport(AnalyticsScopeService.F_CURRENCY, "Currency", true,
                        "Limits invoice and bill based figures to documents raised in that currency. Ledger totals are always in " + base + "."),
                new FilterSupport(AnalyticsScopeService.F_CUSTOMER, "Customer", true, null),
                new FilterSupport(AnalyticsScopeService.F_SUPPLIER, "Supplier", true, null),
                new FilterSupport(AnalyticsScopeService.F_PRODUCT, "Product / service", true, null),
                new FilterSupport(AnalyticsScopeService.F_ACCOUNT, "Account", true, null),
                new FilterSupport(AnalyticsScopeService.F_BRANCH, "Branch / location", false, AnalyticsScopeService.BRANCH_REASON),
                new FilterSupport(AnalyticsScopeService.F_DEPARTMENT, "Department", false, AnalyticsScopeService.DEPARTMENT_REASON));

        List<AnalyticsContextResponse.AccountOption> accounts = new ArrayList<>();
        classifier.classifyAll().values().forEach(a -> accounts.add(new AnalyticsContextResponse.AccountOption(
                a.id(), a.code(), a.name(), a.accountClass().name(), a.accountClass().getLabel())));

        return new AnalyticsContextResponse(today, base, fy, period, new ArrayList<>(currencies), filters, accounts);
    }
}
