package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.AnalyticsFilter;
import com.unionsg.xaccounting.dto.analytics.Breakdown;
import com.unionsg.xaccounting.dto.analytics.BreakdownRow;
import com.unionsg.xaccounting.dto.analytics.DrillTarget;
import com.unionsg.xaccounting.dto.analytics.MetricDto;
import com.unionsg.xaccounting.dto.analytics.Period;
import com.unionsg.xaccounting.dto.analytics.RevenueAnalyticsResponse;
import com.unionsg.xaccounting.dto.analytics.SeriesPoint;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.invoice.InvoiceItem;
import com.unionsg.xaccounting.entity.product.Product;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.ProductItemType;
import com.unionsg.xaccounting.repository.analytics.AnalyticsInvoiceActivityRow;
import com.unionsg.xaccounting.repository.analytics.AnalyticsLedgerRepository;
import com.unionsg.xaccounting.service.analytics.Metrics.Polarity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.unionsg.xaccounting.service.analytics.AnalyticsScopeService.*;

/**
 * Revenue analytics. Revenue is recognised revenue as the ledger has it: net credits to
 * operating revenue accounts from posted journals (reversed journals and their reversals both
 * counted so they cancel). Draft, unsent and cancelled invoices never reach the ledger, so they
 * never count. Customer, product, status and currency splits come from the invoice each revenue
 * journal was posted from; revenue posted any other way (manual journals) shows as
 * "Not from an invoice", so every split adds back to the ledger total.
 */
@Service
@RequiredArgsConstructor
public class RevenueAnalyticsService {

    static final String NOT_FROM_INVOICE = "Not from an invoice";
    static final String FREE_TEXT = "Lines without a product";
    static final String SUPPLIER_REASON = "Revenue does not involve suppliers.";
    static final int MAX_ROWS = 50;

    private final AnalyticsScopeService scopeService;
    private final AnalyticsLedgerRepository ledgerRepository;
    private final AccountClassifier classifier;

    /** One slice of recognised revenue: an account's activity on a day, from one invoice line. */
    record Fact(LocalDate day, ClassifiedAccount account, Invoice invoice, InvoiceItem item, BigDecimal amount) {
    }

    @Transactional(readOnly = true)
    public RevenueAnalyticsResponse build(AnalyticsFilter filter) {
        AnalyticsScopeService.Resolved r = scopeService.resolve(filter,
                Set.of(F_CURRENCY, F_CUSTOMER, F_PRODUCT, F_ACCOUNT), SUPPLIER_REASON,
                "Recognised revenue: net credits to operating revenue accounts from posted journals. Draft and cancelled invoices are never posted, and a reversed journal cancels against its reversal.");
        AnalyticsFilter f = r.filter();
        Period p = r.period();
        Period c = r.comparison();
        List<String> notes = new ArrayList<>();

        Map<Long, ClassifiedAccount> accounts = classifier.classifyAll();
        Set<Long> revenueIds = revenueAccounts(accounts, f.getAccountIds(), notes);

        List<Fact> facts = revenueIds.isEmpty() ? List.of() : facts(r, accounts, revenueIds);

        boolean invoiceScoped = (f.getCurrency() != null && !f.getCurrency().isBlank())
                || !f.getCustomerIds().isEmpty() || !f.getProductIds().isEmpty();
        if (invoiceScoped) {
            String currency = f.getCurrency() == null ? null : f.getCurrency().trim().toUpperCase(Locale.ROOT);
            Set<Long> customers = new HashSet<>(f.getCustomerIds());
            Set<Long> products = new HashSet<>(f.getProductIds());
            facts = facts.stream().filter(x -> x.invoice() != null)
                    .filter(x -> currency == null || currency.equalsIgnoreCase(x.invoice().getCurrency()))
                    .filter(x -> customers.isEmpty() || (x.invoice().getCustomer() != null && customers.contains(x.invoice().getCustomer().getId())))
                    .filter(x -> products.isEmpty() || (x.item() != null && x.item().getProduct() != null && products.contains(x.item().getProduct().getId())))
                    .toList();
            notes.add("Customer, product and currency filters count only revenue posted from invoices; revenue entered as manual journals is left out while they are set.");
        }

        Predicate<Fact> inCur = x -> within(x.day(), p.from(), p.to());
        Predicate<Fact> inPrev = x -> c != null && within(x.day(), c.from(), c.to());

        BigDecimal total = sum(facts, inCur);
        BigDecimal prevTotal = c == null ? null : sum(facts, inPrev);
        MetricDto totalMetric = Metrics.metric("revenue", "Revenue", Metrics.MONEY, total, prevTotal,
                Polarity.HIGHER_IS_BETTER, "Net credits to operating revenue accounts in the period.",
                new DrillTarget("Revenue", List.of(), new ArrayList<>(revenueIds), "PERIOD"), c, r.baseCurrency());

        List<SeriesPoint> series = series(r, facts);

        List<Breakdown> breakdowns = new ArrayList<>();
        breakdowns.add(split("category", "Revenue category", facts, inCur, inPrev, total, c != null,
                x -> x.account().category(), x -> x.account().category(),
                key -> new DrillTarget("Revenue · " + key, List.of(), idsInCategory(accounts, revenueIds, key), "PERIOD")));
        breakdowns.add(split("account", "Revenue account", facts, inCur, inPrev, total, c != null,
                x -> "account:" + x.account().id(), x -> x.account().code() + " · " + x.account().name(),
                key -> new DrillTarget(null, List.of(), List.of(Long.valueOf(key.substring(8))), "PERIOD")));
        breakdowns.add(split("customer", "Customer", facts, inCur, inPrev, total, c != null,
                x -> x.invoice() == null ? "none" : x.invoice().getCustomer() == null ? "unknown" : "customer:" + x.invoice().getCustomer().getId(),
                x -> x.invoice() == null ? NOT_FROM_INVOICE : x.invoice().getCustomer() == null ? "No customer" : x.invoice().getCustomer().getDisplayName(),
                key -> null));
        Predicate<Fact> isService = x -> x.item() != null && x.item().getProduct() != null
                && x.item().getProduct().getItemType() == ProductItemType.SERVICE;
        breakdowns.add(split("product", "Product", facts.stream().filter(isService.negate()).toList(), inCur, inPrev, total, c != null,
                RevenueAnalyticsService::productKey, RevenueAnalyticsService::productLabel, key -> null));
        breakdowns.add(split("service", "Service", facts.stream().filter(isService).toList(), inCur, inPrev, total, c != null,
                RevenueAnalyticsService::productKey, RevenueAnalyticsService::productLabel, key -> null));
        breakdowns.add(split("productCategory", "Product category", facts, inCur, inPrev, total, c != null,
                x -> "cat:" + categoryLabel(x), RevenueAnalyticsService::categoryLabel, key -> null));
        breakdowns.add(split("itemType", "Product or service type", facts, inCur, inPrev, total, c != null,
                x -> "type:" + typeLabel(x), RevenueAnalyticsService::typeLabel, key -> null));
        breakdowns.add(split("invoiceStatus", "Invoice status (today)", facts, inCur, inPrev, total, c != null,
                x -> "status:" + statusLabel(x), RevenueAnalyticsService::statusLabel, key -> null));
        breakdowns.add(split("currency", "Invoice currency", facts, inCur, inPrev, total, c != null,
                x -> "cur:" + (x.invoice() == null ? NOT_FROM_INVOICE : String.valueOf(x.invoice().getCurrency())),
                x -> x.invoice() == null ? NOT_FROM_INVOICE : String.valueOf(x.invoice().getCurrency()), key -> null));
        breakdowns.add(new Breakdown("branch", "Branch", false, BRANCH_REASON, List.of()));
        breakdowns.add(new Breakdown("department", "Department", false, DEPARTMENT_REASON, List.of()));
        breakdowns.add(new Breakdown("salesperson", "Salesperson", false, "Invoices do not record a salesperson yet.", List.of()));

        Set<String> currencies = facts.stream().filter(x -> x.invoice() != null).map(x -> x.invoice().getCurrency())
                .filter(Objects::nonNull).map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        currencies.remove(r.baseCurrency());
        if (!currencies.isEmpty()) {
            notes.add("Some invoices were raised in " + String.join(", ", currencies) + ". The ledger holds their amounts as posted, without conversion to "
                    + r.baseCurrency() + ", so totals mix currencies until exchange rates are captured on invoices.");
        }
        return new RevenueAnalyticsResponse(r.scope(), totalMetric, series, breakdowns, notes);
    }

    private Set<Long> revenueAccounts(Map<Long, ClassifiedAccount> accounts, List<Long> requested, List<String> notes) {
        if (requested == null || requested.isEmpty()) {
            return AccountClassifier.idsOf(accounts, List.of(AccountClass.OPERATING_REVENUE));
        }
        Set<Long> ids = new HashSet<>();
        for (Long id : requested) {
            ClassifiedAccount a = accounts.get(id);
            if (a != null && a.accountType() == AccountType.INCOME) ids.add(id);
        }
        if (ids.size() < requested.size()) {
            notes.add("Only income accounts apply to revenue; other selected accounts were left out.");
        }
        return ids;
    }

    private List<Fact> facts(AnalyticsScopeService.Resolved r, Map<Long, ClassifiedAccount> accounts, Set<Long> revenueIds) {
        List<AnalyticsInvoiceActivityRow> rows = ledgerRepository.findActivityByInvoice(r.earliest(), r.latest(), revenueIds);
        Set<Long> invoiceIds = rows.stream().map(AnalyticsInvoiceActivityRow::getInvoiceId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Invoice> invoices = new HashMap<>();
        if (!invoiceIds.isEmpty()) {
            ledgerRepository.findInvoicesWithItems(invoiceIds).forEach(i -> invoices.put(i.getId(), i));
        }
        List<Fact> out = new ArrayList<>();
        for (AnalyticsInvoiceActivityRow row : rows) {
            ClassifiedAccount account = accounts.get(row.getAccountId());
            BigDecimal amount = LedgerData.nz(row.getCredit()).subtract(LedgerData.nz(row.getDebit()));
            if (account == null || amount.signum() == 0) continue;
            Invoice inv = row.getInvoiceId() == null ? null : invoices.get(row.getInvoiceId());
            if (inv == null) {
                out.add(new Fact(row.getDay(), account, null, null, amount));
                continue;
            }
            out.addAll(allocate(row.getDay(), account, inv, amount));
        }
        return out;
    }

    /**
     * Spreads an invoice's revenue on one account over its lines by line subtotal. Lines whose
     * product posts to a different income account are kept on that account when possible.
     */
    static List<Fact> allocate(LocalDate day, ClassifiedAccount account, Invoice inv, BigDecimal amount) {
        List<InvoiceItem> items = inv.getItems() == null ? List.of() : inv.getItems();
        List<InvoiceItem> onAccount = items.stream()
                .filter(it -> it.getProduct() != null && it.getProduct().getIncomeAccount() != null
                        && Objects.equals(it.getProduct().getIncomeAccount().getId(), account.id()))
                .toList();
        List<InvoiceItem> pool = !onAccount.isEmpty() ? onAccount : items.stream()
                .filter(it -> it.getProduct() == null || it.getProduct().getIncomeAccount() == null)
                .toList();
        if (pool.isEmpty()) pool = items;
        BigDecimal base = pool.stream().map(it -> LedgerData.nz(it.getLineSubtotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (pool.isEmpty() || base.signum() == 0) {
            return List.of(new Fact(day, account, inv, null, amount));
        }
        List<Fact> out = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < pool.size(); i++) {
            InvoiceItem it = pool.get(i);
            BigDecimal share = i == pool.size() - 1 ? amount.subtract(allocated)
                    : amount.multiply(LedgerData.nz(it.getLineSubtotal())).divide(base, 2, RoundingMode.HALF_UP);
            allocated = allocated.add(share);
            out.add(new Fact(day, account, inv, it, share));
        }
        return out;
    }

    private List<SeriesPoint> series(AnalyticsScopeService.Resolved r, List<Fact> facts) {
        List<Buckets.Bucket> cur = Buckets.of(r.period().from(), r.period().to(), r.scope().granularity());
        List<Buckets.Bucket> prev = r.comparison() == null ? List.of()
                : Buckets.of(r.comparison().from(), r.comparison().to(), r.scope().granularity());
        return ExecutiveDashboardService.trend("revenue", "Revenue", cur, prev,
                (s, e) -> sum(facts, x -> within(x.day(), s, e)), null).points();
    }

    static Breakdown split(String key, String label, List<Fact> facts, Predicate<Fact> inCur, Predicate<Fact> inPrev,
                           BigDecimal total, boolean compare, Function<Fact, String> keyOf, Function<Fact, String> labelOf,
                           Function<String, DrillTarget> drillOf) {
        Map<String, BigDecimal[]> sums = new LinkedHashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (Fact x : facts) {
            boolean cur = inCur.test(x);
            boolean prev = inPrev.test(x);
            if (!cur && !prev) continue;
            String k = keyOf.apply(x);
            labels.putIfAbsent(k, labelOf.apply(x));
            BigDecimal[] s = sums.computeIfAbsent(k, z -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (cur) s[0] = s[0].add(x.amount());
            if (prev) s[1] = s[1].add(x.amount());
        }
        List<BreakdownRow> rows = sums.entrySet().stream()
                .map(e -> {
                    BigDecimal cv = Metrics.scale(e.getValue()[0]);
                    BigDecimal pv = compare ? Metrics.scale(e.getValue()[1]) : null;
                    return new BreakdownRow(e.getKey(), labels.get(e.getKey()), cv, pv,
                            pv == null ? null : cv.subtract(pv), Metrics.growth(cv, pv), Metrics.pct(cv, total),
                            drillOf.apply(e.getKey()));
                })
                .filter(row -> row.current().signum() != 0 || (row.previous() != null && row.previous().signum() != 0))
                .sorted(Comparator.comparing(BreakdownRow::current).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
        if (rows.size() > MAX_ROWS) {
            List<BreakdownRow> rest = rows.subList(MAX_ROWS - 1, rows.size());
            BigDecimal rc = rest.stream().map(BreakdownRow::current).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal rp = compare ? rest.stream().map(x -> LedgerData.nz(x.previous())).reduce(BigDecimal.ZERO, BigDecimal::add) : null;
            int n = rest.size();
            rows = new ArrayList<>(rows.subList(0, MAX_ROWS - 1));
            rows.add(new BreakdownRow("other", "Other (" + n + ")", rc, rp, rp == null ? null : rc.subtract(rp),
                    Metrics.growth(rc, rp), Metrics.pct(rc, total), null));
        }
        return new Breakdown(key, label, true, null, rows);
    }

    private static List<Long> idsInCategory(Map<Long, ClassifiedAccount> accounts, Set<Long> revenueIds, String category) {
        return revenueIds.stream().filter(id -> accounts.get(id) != null && category.equals(accounts.get(id).category())).toList();
    }

    static String productKey(Fact x) {
        if (x.invoice() == null) return "none";
        if (x.item() == null || x.item().getProduct() == null) return "free";
        return "product:" + x.item().getProduct().getId();
    }

    static String productLabel(Fact x) {
        if (x.invoice() == null) return NOT_FROM_INVOICE;
        if (x.item() == null || x.item().getProduct() == null) return FREE_TEXT;
        return x.item().getProduct().getName();
    }

    static String categoryLabel(Fact x) {
        if (x.invoice() == null) return NOT_FROM_INVOICE;
        Product product = x.item() == null ? null : x.item().getProduct();
        if (product == null) return FREE_TEXT;
        return product.getCategory() == null || product.getCategory().isBlank() ? "Uncategorised" : product.getCategory();
    }

    static String typeLabel(Fact x) {
        if (x.invoice() == null) return NOT_FROM_INVOICE;
        Product product = x.item() == null ? null : x.item().getProduct();
        if (product == null || product.getItemType() == null) return FREE_TEXT;
        return product.getItemType().getLabel();
    }

    static String statusLabel(Fact x) {
        if (x.invoice() == null || x.invoice().getStatus() == null) return NOT_FROM_INVOICE;
        String s = x.invoice().getStatus().name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static BigDecimal sum(List<Fact> facts, Predicate<Fact> which) {
        BigDecimal t = BigDecimal.ZERO;
        for (Fact x : facts) if (which.test(x)) t = t.add(x.amount());
        return t;
    }

    static boolean within(LocalDate d, LocalDate from, LocalDate to) {
        return !d.isBefore(from) && !d.isAfter(to);
    }
}
