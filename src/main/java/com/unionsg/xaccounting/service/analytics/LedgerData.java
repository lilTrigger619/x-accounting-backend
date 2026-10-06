package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.repository.analytics.AnalyticsBalanceRow;
import com.unionsg.xaccounting.repository.analytics.AnalyticsDailyRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Ledger activity loaded once for a response (covering both the period and its comparison),
 * from which every figure on that response is derived. Amounts come back in each class's
 * natural sign: revenue and liabilities positive when in credit, assets and expenses positive
 * when in debit.
 */
public final class LedgerData {

    private final Map<Long, ClassifiedAccount> accounts;
    private final LocalDate loadedFrom;
    private final Map<Long, BigDecimal> openingNet = new HashMap<>();
    private final List<AnalyticsDailyRow> daily;

    LedgerData(Map<Long, ClassifiedAccount> accounts, LocalDate loadedFrom,
               List<AnalyticsBalanceRow> opening, List<AnalyticsDailyRow> daily) {
        this.accounts = accounts;
        this.loadedFrom = loadedFrom;
        this.daily = daily;
        for (AnalyticsBalanceRow row : opening) {
            openingNet.merge(row.getAccountId(), nz(row.getDebit()).subtract(nz(row.getCredit())), BigDecimal::add);
        }
    }

    public Map<Long, ClassifiedAccount> accounts() {
        return accounts;
    }

    /** Activity over [from, to] for accounts in the given classes. */
    public BigDecimal activity(Collection<AccountClass> classes, LocalDate from, LocalDate to) {
        return activity(a -> classes.contains(a.accountClass()), from, to);
    }

    public BigDecimal activity(Predicate<ClassifiedAccount> which, LocalDate from, LocalDate to) {
        BigDecimal total = BigDecimal.ZERO;
        for (AnalyticsDailyRow row : daily) {
            if (row.getDay().isBefore(from) || row.getDay().isAfter(to)) continue;
            ClassifiedAccount a = accounts.get(row.getAccountId());
            if (a == null || !which.test(a)) continue;
            total = total.add(natural(a, nz(row.getDebit()).subtract(nz(row.getCredit()))));
        }
        return total;
    }

    /** Balance at the end of {@code date} for accounts in the given classes. */
    public BigDecimal balanceAt(Collection<AccountClass> classes, LocalDate date) {
        return balanceAt(a -> classes.contains(a.accountClass()), date);
    }

    public BigDecimal balanceAt(Predicate<ClassifiedAccount> which, LocalDate date) {
        if (date.isBefore(loadedFrom.minusDays(1))) {
            throw new IllegalStateException("Balance requested before the loaded range");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> e : openingNet.entrySet()) {
            ClassifiedAccount a = accounts.get(e.getKey());
            if (a != null && which.test(a)) total = total.add(natural(a, e.getValue()));
        }
        for (AnalyticsDailyRow row : daily) {
            if (row.getDay().isAfter(date)) continue;
            ClassifiedAccount a = accounts.get(row.getAccountId());
            if (a == null || !which.test(a)) continue;
            total = total.add(natural(a, nz(row.getDebit()).subtract(nz(row.getCredit()))));
        }
        return total;
    }

    /** Per-account activity (or balance when {@code balance} is true) for the accounts matched. */
    public Map<Long, BigDecimal> byAccount(Set<Long> accountIds, LocalDate from, LocalDate to, boolean balance) {
        Map<Long, BigDecimal> out = new HashMap<>();
        if (balance) {
            for (Map.Entry<Long, BigDecimal> e : openingNet.entrySet()) {
                ClassifiedAccount a = accounts.get(e.getKey());
                if (a != null && accountIds.contains(a.id())) out.merge(a.id(), natural(a, e.getValue()), BigDecimal::add);
            }
        }
        for (AnalyticsDailyRow row : daily) {
            if (row.getDay().isAfter(to) || (!balance && row.getDay().isBefore(from))) continue;
            ClassifiedAccount a = accounts.get(row.getAccountId());
            if (a == null || !accountIds.contains(a.id())) continue;
            out.merge(a.id(), natural(a, nz(row.getDebit()).subtract(nz(row.getCredit()))), BigDecimal::add);
        }
        return out;
    }

    static BigDecimal natural(ClassifiedAccount a, BigDecimal debitMinusCredit) {
        return a.accountClass().isDebitNatured() ? debitMinusCredit : debitMinusCredit.negate();
    }

    static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
