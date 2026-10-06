package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.DrillAccountsResponse;
import com.unionsg.xaccounting.dto.analytics.DrillLinesResponse;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.analytics.AnalyticsLedgerRepository;
import com.unionsg.xaccounting.repository.analytics.AnalyticsLineRow;
import com.unionsg.xaccounting.service.banking.BaseCurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Drill-down from any BI figure: figure → category → account → journal entry → source
 * transaction. Uses the same ledger rules and classes as the figure itself, so the totals agree.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsDrillService {

    private static final LocalDate SINCE_INCEPTION = LocalDate.of(1900, 1, 1);

    private final LedgerDataService ledgerDataService;
    private final AccountClassifier classifier;
    private final AnalyticsLedgerRepository ledgerRepository;
    private final BaseCurrencyService baseCurrencyService;

    @Transactional(readOnly = true)
    public DrillAccountsResponse accounts(List<String> classes, List<Long> accountIds, LocalDate from, LocalDate to,
                                          String basis, String label) {
        boolean balance = isBalance(basis);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.withDayOfYear(1);
        if (start.isAfter(end)) throw new BusinessException("The start date must be on or before the end date.");

        LedgerData data = ledgerDataService.load(start, end);
        Set<Long> ids = select(data.accounts(), classes, accountIds);
        Map<Long, BigDecimal> amounts = data.byAccount(ids, start, end, balance);

        Map<String, List<DrillAccountsResponse.Account>> byCategory = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ClassifiedAccount a : data.accounts().values()) {
            if (!ids.contains(a.id())) continue;
            BigDecimal amount = signed(a, amounts.getOrDefault(a.id(), BigDecimal.ZERO), classes);
            if (amount.signum() == 0) continue;
            total = total.add(amount);
            byCategory.computeIfAbsent(a.category(), k -> new ArrayList<>()).add(new DrillAccountsResponse.Account(
                    a.id(), a.code(), a.name(), a.accountClass().name(), a.accountClass().getLabel(), Metrics.scale(amount)));
        }
        List<DrillAccountsResponse.Category> categories = byCategory.entrySet().stream()
                .map(e -> new DrillAccountsResponse.Category(e.getKey(),
                        Metrics.scale(e.getValue().stream().map(DrillAccountsResponse.Account::amount).reduce(BigDecimal.ZERO, BigDecimal::add)),
                        e.getValue()))
                .sorted(Comparator.comparing((DrillAccountsResponse.Category cat) -> cat.amount().abs()).reversed())
                .toList();
        return new DrillAccountsResponse(label, balance ? null : start, end, balance ? "BALANCE" : "PERIOD",
                baseCurrencyService.resolve(), Metrics.scale(total), categories);
    }

    @Transactional(readOnly = true)
    public DrillLinesResponse lines(Long accountId, LocalDate from, LocalDate to, String basis, int page, int size) {
        boolean balance = isBalance(basis);
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = balance ? null : (from != null ? from : end.withDayOfYear(1));
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);

        Map<Long, ClassifiedAccount> accounts = classifier.classifyAll();
        ClassifiedAccount account = accounts.get(accountId);
        if (account == null) throw new BusinessException("Account not found: " + accountId);

        List<Long> ids = List.of(accountId);
        LocalDate queryFrom = start != null ? start : SINCE_INCEPTION;
        long count = ledgerRepository.countLines(queryFrom, end, ids);
        List<AnalyticsLineRow> rows = ledgerRepository.findLines(queryFrom, end, ids, PageRequest.of(safePage, safeSize));

        LedgerData data = ledgerDataService.load(start != null ? start : end, end);
        BigDecimal total = data.byAccount(Set.of(accountId), start != null ? start : end, end, balance)
                .getOrDefault(accountId, BigDecimal.ZERO);

        List<DrillLinesResponse.Line> lines = rows.stream().map(row -> {
            BigDecimal debit = LedgerData.nz(row.getDebit());
            BigDecimal credit = LedgerData.nz(row.getCredit());
            boolean inherited = row.getSourceModule() == null && row.getOriginalSourceModule() != null;
            return new DrillLinesResponse.Line(row.getLineId(), row.getJournalId(), row.getJournalNumber(), row.getJournalDate(),
                    row.getStatus() == null ? null : row.getStatus().name(),
                    row.getLineDescription() != null && !row.getLineDescription().isBlank() ? row.getLineDescription() : row.getJournalDescription(),
                    row.getReference(), debit, credit, Metrics.scale(LedgerData.natural(account, debit.subtract(credit))),
                    inherited ? row.getOriginalSourceModule() : row.getSourceModule(),
                    inherited ? row.getOriginalSourceEntityId() : row.getSourceEntityId(),
                    row.getReversalOfJournalId());
        }).toList();

        return new DrillLinesResponse(account.id(), account.code(), account.name(), account.accountClass().getLabel(),
                start, end, Metrics.scale(total), count, safePage, safeSize, lines);
    }

    private static Set<Long> select(Map<Long, ClassifiedAccount> accounts, List<String> classes, List<Long> accountIds) {
        Set<Long> ids = new HashSet<>();
        if (accountIds != null) accountIds.stream().filter(accounts::containsKey).forEach(ids::add);
        if (classes != null && !classes.isEmpty()) {
            Set<AccountClass> wanted = new HashSet<>();
            for (String c : classes) {
                try {
                    wanted.add(AccountClass.valueOf(c.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    throw new BusinessException("Unknown account class: " + c);
                }
            }
            ids.addAll(AccountClassifier.idsOf(accounts, wanted));
        }
        if (ids.isEmpty() && (accountIds == null || accountIds.isEmpty()) && (classes == null || classes.isEmpty())) {
            throw new BusinessException("Choose a figure, class or account to drill into.");
        }
        return ids;
    }

    /**
     * When a drill mixes classes with opposite effects on the figure (gross profit = revenue − cost
     * of sales; working capital = assets − liabilities), the subtracted side is shown negative so
     * the accounts add up to the figure.
     */
    private static BigDecimal signed(ClassifiedAccount a, BigDecimal natural, List<String> classes) {
        if (classes == null || classes.size() < 2) return natural;
        Set<String> wanted = new HashSet<>(classes);
        boolean hasIncome = wanted.contains("OPERATING_REVENUE") || wanted.contains("OTHER_INCOME");
        boolean hasAssets = wanted.contains("CASH_BANK") || wanted.contains("RECEIVABLE") || wanted.contains("OTHER_CURRENT_ASSET");
        AccountClass c = a.accountClass();
        boolean expenseSide = c == AccountClass.COST_OF_SALES || c == AccountClass.OPERATING_EXPENSE || c == AccountClass.OTHER_EXPENSE;
        boolean liabilitySide = c == AccountClass.PAYABLE || c == AccountClass.LOAN_LIABILITY || c == AccountClass.OTHER_CURRENT_LIABILITY;
        if ((hasIncome && expenseSide) || (hasAssets && liabilitySide)) return natural.negate();
        return natural;
    }

    private static boolean isBalance(String basis) {
        return "BALANCE".equalsIgnoreCase(basis);
    }
}
