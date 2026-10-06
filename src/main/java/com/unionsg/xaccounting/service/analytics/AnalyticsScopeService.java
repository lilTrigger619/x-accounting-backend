package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.AnalyticsFilter;
import com.unionsg.xaccounting.dto.analytics.AnalyticsScope;
import com.unionsg.xaccounting.dto.analytics.CompareMode;
import com.unionsg.xaccounting.dto.analytics.Granularity;
import com.unionsg.xaccounting.dto.analytics.IgnoredFilter;
import com.unionsg.xaccounting.dto.analytics.Period;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.service.banking.BaseCurrencyService;
import com.unionsg.xaccounting.service.reports.CurrentFiscalPeriodResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Turns the global filter bar into the one period, comparison and filter set a response uses. */
@Service
@RequiredArgsConstructor
public class AnalyticsScopeService {

    static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final int MAX_BUCKETS = 400;

    public static final String F_CURRENCY = "currency";
    public static final String F_CUSTOMER = "customer";
    public static final String F_SUPPLIER = "supplier";
    public static final String F_PRODUCT = "product";
    public static final String F_ACCOUNT = "account";
    public static final String F_BRANCH = "branch";
    public static final String F_DEPARTMENT = "department";

    static final String BRANCH_REASON = "Transactions do not record a branch or location yet.";
    static final String DEPARTMENT_REASON = "Invoices, bills and journals do not record a department yet.";

    private final CurrentFiscalPeriodResolver fiscalPeriodResolver;
    private final BaseCurrencyService baseCurrencyService;

    public record Resolved(AnalyticsScope scope, AnalyticsFilter filter, LocalDate today) {
        public Period period() {
            return scope.period();
        }

        public Period comparison() {
            return scope.comparison();
        }

        public LocalDate earliest() {
            return comparison() != null && comparison().from().isBefore(period().from()) ? comparison().from() : period().from();
        }

        public LocalDate latest() {
            return comparison() != null && comparison().to().isAfter(period().to()) ? comparison().to() : period().to();
        }

        public String baseCurrency() {
            return scope.baseCurrency();
        }
    }

    /**
     * @param supported the filters this screen applies; any other filter the user set is listed
     *                  as ignored, with {@code reasonWhenUnsupported}, so the screen can show it.
     */
    public Resolved resolve(AnalyticsFilter in, Set<String> supported, String reasonWhenUnsupported, String basis) {
        AnalyticsFilter f = in == null ? new AnalyticsFilter() : in;
        LocalDate today = LocalDate.now();
        LocalDate to = f.getTo() != null ? f.getTo() : today;
        LocalDate from = f.getFrom() != null ? f.getFrom() : fiscalPeriodResolver.resolveYearStart(to);
        if (from.isAfter(to)) {
            throw new BusinessException("The start date must be on or before the end date.");
        }
        Period period = new Period(from, to, label(from, to));

        CompareMode mode = f.getCompare() == null ? CompareMode.PREVIOUS_PERIOD : f.getCompare();
        Period comparison = switch (mode) {
            case NONE -> null;
            case PREVIOUS_YEAR -> {
                LocalDate pf = from.minusYears(1);
                LocalDate pt = to.minusYears(1);
                yield new Period(pf, pt, label(pf, pt));
            }
            case CUSTOM -> {
                if (f.getCompareFrom() == null || f.getCompareTo() == null) {
                    throw new BusinessException("A custom comparison needs both a start and an end date.");
                }
                if (f.getCompareFrom().isAfter(f.getCompareTo())) {
                    throw new BusinessException("The comparison start date must be on or before its end date.");
                }
                yield new Period(f.getCompareFrom(), f.getCompareTo(), label(f.getCompareFrom(), f.getCompareTo()));
            }
            case PREVIOUS_PERIOD -> previousPeriod(from, to);
        };

        List<String> notes = new ArrayList<>();
        Granularity granularity = f.getGranularity() != null ? f.getGranularity() : defaultGranularity(from, to);
        while (Buckets.count(from, to, granularity) > MAX_BUCKETS && granularity != Granularity.YEAR) {
            granularity = Granularity.values()[granularity.ordinal() + 1];
        }

        List<String> applied = new ArrayList<>();
        List<IgnoredFilter> ignored = new ArrayList<>();
        check(f.getCurrency() != null && !f.getCurrency().isBlank(), F_CURRENCY, "Currency", supported, reasonWhenUnsupported, applied, ignored);
        check(!f.getCustomerIds().isEmpty(), F_CUSTOMER, "Customer", supported, reasonWhenUnsupported, applied, ignored);
        check(!f.getSupplierIds().isEmpty(), F_SUPPLIER, "Supplier", supported, reasonWhenUnsupported, applied, ignored);
        check(!f.getProductIds().isEmpty(), F_PRODUCT, "Product / service", supported, reasonWhenUnsupported, applied, ignored);
        check(!f.getAccountIds().isEmpty(), F_ACCOUNT, "Account", supported, reasonWhenUnsupported, applied, ignored);
        if (f.getBranchId() != null) ignored.add(new IgnoredFilter("Branch / location", BRANCH_REASON));
        if (f.getDepartmentId() != null) ignored.add(new IgnoredFilter("Department", DEPARTMENT_REASON));

        AnalyticsScope scope = new AnalyticsScope(period, comparison, mode, granularity,
                baseCurrencyService.resolve(), applied, ignored, basis);
        return new Resolved(scope, f, today);
    }

    private static void check(boolean set, String key, String label, Set<String> supported, String reason,
                              List<String> applied, List<IgnoredFilter> ignored) {
        if (!set) return;
        if (supported.contains(key)) applied.add(label);
        else ignored.add(new IgnoredFilter(label, reason));
    }

    /**
     * The period of the same length just before this one. Whole calendar months shift by whole
     * months (so Mar 1–31 compares with Feb 1–28, not Jan 29–Feb 28).
     */
    static Period previousPeriod(LocalDate from, LocalDate to) {
        boolean wholeMonths = from.getDayOfMonth() == 1 && to.equals(to.withDayOfMonth(to.lengthOfMonth()));
        LocalDate pf;
        LocalDate pt;
        if (wholeMonths) {
            long months = ChronoUnit.MONTHS.between(from, to.plusDays(1));
            pf = from.minusMonths(months);
            pt = from.minusDays(1);
        } else {
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            pt = from.minusDays(1);
            pf = pt.minusDays(days - 1);
        }
        return new Period(pf, pt, label(pf, pt));
    }

    static Granularity defaultGranularity(LocalDate from, LocalDate to) {
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days <= 31) return Granularity.DAY;
        if (days <= 120) return Granularity.WEEK;
        if (days <= 1100) return Granularity.MONTH;
        return Granularity.QUARTER;
    }

    public static String label(LocalDate from, LocalDate to) {
        return from.format(LABEL) + " – " + to.format(LABEL);
    }
}
