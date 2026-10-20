package com.unionsg.xaccounting.service.reports;

import com.unionsg.xaccounting.dto.reports.cashflow.CashFlowStatementResponse;
import com.unionsg.xaccounting.dto.reports.cashflow.CashFlowStatementResponse.AccountAmount;
import com.unionsg.xaccounting.dto.reports.cashflow.CashFlowStatementResponse.Line;
import com.unionsg.xaccounting.dto.reports.cashflow.CashFlowStatementResponse.Section;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.settings.MappingKey;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.analytics.AnalyticsBalanceRow;
import com.unionsg.xaccounting.repository.analytics.AnalyticsLedgerRepository;
import com.unionsg.xaccounting.repository.reports.CashFlowLedgerRepository;
import com.unionsg.xaccounting.repository.reports.CashFlowLedgerRepository.JournalAccountRow;
import com.unionsg.xaccounting.service.analytics.AccountClass;
import com.unionsg.xaccounting.service.analytics.AccountClassifier;
import com.unionsg.xaccounting.service.analytics.ClassifiedAccount;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetAccountRules;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetAccountRules.Role;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetJournalService;
import com.unionsg.xaccounting.service.settings.AccountingMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Indirect-method cash flow statement built from the same posted ledger as the Balance Sheet and
 * P&L. Every balance sheet account other than cash falls in exactly one bucket, so
 * operating + investing + financing always equals the change in cash:
 * <ul>
 *   <li><b>Operating</b>: net profit, add-backs for the P&L lines the fixed asset module posted
 *       (depreciation, impairment, gain or loss on disposal), any other non-cash fixed asset
 *       movement, and working capital changes.</li>
 *   <li><b>Investing</b>: only the cash legs of journals that move fixed asset accounts (purchases
 *       and disposal proceeds), and loans advanced.</li>
 *   <li><b>Financing</b>: borrowings, other non-current liabilities and equity.</li>
 * </ul>
 * Year-end closing journals only move profit into retained earnings, so they are left out.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CashFlowStatementService {

    private static final Set<String> MODULE_SOURCES = Set.of(FixedAssetJournalService.SOURCE_ASSET, FixedAssetJournalService.SOURCE_RUN);

    private final CashFlowLedgerRepository ledgerRepository;
    private final AnalyticsLedgerRepository balanceRepository;
    private final AccountClassifier classifier;
    private final FixedAssetAccountRules fixedAssetAccounts;
    private final AccountingMappingService mappingService;
    private final AccountRepository accountRepository;

    public CashFlowStatementResponse generate(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new BadRequestException("fromDate and toDate are required");
        if (from.isAfter(to)) throw new BadRequestException("fromDate cannot be after toDate");

        Map<Long, ClassifiedAccount> accounts = classifier.classifyAll();
        Map<Role, Set<Long>> roles = fixedAssetAccounts.configuredAccountIds();
        Set<Long> fixedAssetIds = new HashSet<>();
        fixedAssetIds.addAll(roles.get(Role.COST));
        fixedAssetIds.addAll(roles.get(Role.ACCUMULATED_DEPRECIATION));
        fixedAssetIds.addAll(roles.get(Role.ACCUMULATED_IMPAIRMENT));
        fixedAssetIds.addAll(AccountClassifier.idsOf(accounts, List.of(AccountClass.NON_CURRENT_ASSET)));
        Set<Long> lentLoanIds = mappedIds(MappingKey.LOAN_RECEIVABLE);
        fixedAssetIds.removeAll(lentLoanIds);

        // Group the period's activity by journal.
        Map<Long, List<JournalAccountRow>> journals = new LinkedHashMap<>();
        for (JournalAccountRow row : ledgerRepository.findJournalAccountActivity(from, to)) {
            if (row.getJournalType() == JournalType.CLOSING) continue;
            journals.computeIfAbsent(row.getJournalId(), k -> new ArrayList<>()).add(row);
        }

        Map<Long, BigDecimal> movement = new HashMap<>();     // debit - credit per account
        Map<Long, BigDecimal> addBacks = new HashMap<>();     // module P&L lines per account
        Map<Long, BigDecimal> purchases = new HashMap<>();    // cash paid per cash account
        Map<Long, BigDecimal> proceeds = new HashMap<>();     // cash received per cash account
        for (List<JournalAccountRow> rows : journals.values()) {
            boolean module = MODULE_SOURCES.contains(rows.get(0).getSourceModule());
            BigDecimal cash = BigDecimal.ZERO;
            BigDecimal fixedAssets = BigDecimal.ZERO;
            for (JournalAccountRow r : rows) {
                BigDecimal net = r.getDebit().subtract(r.getCredit());
                movement.merge(r.getAccountId(), net, BigDecimal::add);
                ClassifiedAccount a = accounts.get(r.getAccountId());
                if (a == null) continue;
                if (a.accountClass() == AccountClass.CASH_BANK) cash = cash.add(net);
                if (fixedAssetIds.contains(r.getAccountId())) fixedAssets = fixedAssets.add(net);
                if (module && !a.accountClass().isBalanceSheet()) addBacks.merge(r.getAccountId(), net, BigDecimal::add);
            }
            if (cash.signum() == 0 || (!module && fixedAssets.signum() == 0)) continue;
            // In a module journal every cash leg is investing. In any other journal only the part of
            // the cash leg that pays for (or is received for) fixed assets is.
            BigDecimal investing = module ? cash
                    : cash.signum() != fixedAssets.signum() ? cash.abs().min(fixedAssets.abs()).multiply(BigDecimal.valueOf(cash.signum()))
                    : BigDecimal.ZERO;
            if (investing.signum() == 0) continue;
            spread(rows, accounts, cash, investing, investing.signum() < 0 ? purchases : proceeds);
        }

        BigDecimal netProfit = BigDecimal.ZERO;
        Map<String, List<AccountAmount>> buckets = new LinkedHashMap<>();
        List<AccountAmount> cashAccounts = new ArrayList<>();
        Map<Long, BigDecimal> opening = balances(balanceRepository.findBalancesBefore(from));
        Map<Long, BigDecimal> closing = balances(balanceRepository.findBalancesBefore(to.plusDays(1)));
        BigDecimal fixedAssetMovement = BigDecimal.ZERO;
        List<AccountAmount> fixedAssetAccountsMoved = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : movement.entrySet()) {
            ClassifiedAccount a = accounts.get(e.getKey());
            if (a == null) continue;
            BigDecimal net = e.getValue();
            if (!a.accountClass().isBalanceSheet()) {
                netProfit = netProfit.subtract(net);
                continue;
            }
            if (a.accountClass() == AccountClass.CASH_BANK) continue;
            AccountAmount amount = amount(a, net.negate(), opening, closing);
            if (fixedAssetIds.contains(a.id())) {
                fixedAssetMovement = fixedAssetMovement.add(net);
                fixedAssetAccountsMoved.add(amount);
            } else if (lentLoanIds.contains(a.id())) {
                buckets.computeIfAbsent("LOANS_ADVANCED", k -> new ArrayList<>()).add(amount);
            } else {
                buckets.computeIfAbsent(bucket(a.accountClass()), k -> new ArrayList<>()).add(amount);
            }
        }
        for (ClassifiedAccount a : accounts.values()) {
            if (a.accountClass() != AccountClass.CASH_BANK) continue;
            BigDecimal o = opening.getOrDefault(a.id(), BigDecimal.ZERO);
            BigDecimal c = closing.getOrDefault(a.id(), BigDecimal.ZERO);
            if (o.signum() == 0 && c.signum() == 0 && !movement.containsKey(a.id())) continue;
            cashAccounts.add(new AccountAmount(a.id(), a.code(), a.name(), c.subtract(o), o, c));
        }

        // Operating
        List<Line> operating = new ArrayList<>();
        operating.add(new Line("NET_PROFIT", "Net profit for the period", netProfit, List.of()));
        BigDecimal addBackTotal = BigDecimal.ZERO;
        Map<String, List<AccountAmount>> addBackGroups = new LinkedHashMap<>();
        for (Map.Entry<Long, BigDecimal> e : addBacks.entrySet()) {
            ClassifiedAccount a = accounts.get(e.getKey());
            addBackTotal = addBackTotal.add(e.getValue());
            String key = roles.get(Role.DEPRECIATION_EXPENSE).contains(a.id()) ? "DEPRECIATION"
                    : roles.get(Role.IMPAIRMENT_LOSS).contains(a.id()) ? "IMPAIRMENT"
                    : roles.get(Role.DISPOSAL_GAIN).contains(a.id()) || roles.get(Role.DISPOSAL_LOSS).contains(a.id()) ? "DISPOSAL_GAIN_LOSS"
                    : a.accountType() == AccountType.EXPENSE ? "DEPRECIATION" : "DISPOSAL_GAIN_LOSS";
            addBackGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(amount(a, e.getValue(), opening, closing));
        }
        addLine(operating, "DEPRECIATION", "Depreciation", addBackGroups.get("DEPRECIATION"));
        addLine(operating, "IMPAIRMENT", "Impairment losses", addBackGroups.get("IMPAIRMENT"));
        addLine(operating, "DISPOSAL_GAIN_LOSS", "(Gain) / loss on disposal of fixed assets", addBackGroups.get("DISPOSAL_GAIN_LOSS"));

        BigDecimal investingCash = sum(purchases).add(sum(proceeds));
        BigDecimal nonCashFixedAssets = fixedAssetMovement.negate().subtract(investingCash).subtract(addBackTotal);
        if (nonCashFixedAssets.signum() != 0) {
            operating.add(new Line("NON_CASH_FIXED_ASSETS",
                    "Non-cash fixed asset movements (acquisitions not paid in cash, transfers)", nonCashFixedAssets,
                    fixedAssetAccountsMoved));
        }
        addLine(operating, "RECEIVABLES", "(Increase) / decrease in receivables", buckets.get("RECEIVABLES"));
        addLine(operating, "OTHER_CURRENT_ASSETS", "(Increase) / decrease in other current assets", buckets.get("OTHER_CURRENT_ASSETS"));
        addLine(operating, "PAYABLES", "Increase / (decrease) in payables", buckets.get("PAYABLES"));
        addLine(operating, "OTHER_CURRENT_LIABILITIES", "Increase / (decrease) in other current liabilities", buckets.get("OTHER_CURRENT_LIABILITIES"));

        // Investing
        List<Line> investing = new ArrayList<>();
        addLine(investing, "FIXED_ASSET_PURCHASES", "Purchase of fixed assets", cashLines(purchases, accounts, opening, closing));
        addLine(investing, "FIXED_ASSET_PROCEEDS", "Proceeds from disposal of fixed assets", cashLines(proceeds, accounts, opening, closing));
        addLine(investing, "LOANS_ADVANCED", "Loans advanced, net of repayments", buckets.get("LOANS_ADVANCED"));

        // Financing
        List<Line> financing = new ArrayList<>();
        addLine(financing, "BORROWINGS", "Borrowings received, net of repayments", buckets.get("BORROWINGS"));
        addLine(financing, "NON_CURRENT_LIABILITIES", "Other non-current liabilities", buckets.get("NON_CURRENT_LIABILITIES"));
        addLine(financing, "EQUITY", "Equity contributed, net of drawings and dividends", buckets.get("EQUITY"));

        List<Section> sections = List.of(
                section("OPERATING", "Cash flows from operating activities", operating),
                section("INVESTING", "Cash flows from investing activities", investing),
                section("FINANCING", "Cash flows from financing activities", financing));
        BigDecimal netCashFlow = sections.stream().map(Section::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal openingCash = cashAccounts.stream().map(AccountAmount::openingBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal closingCash = cashAccounts.stream().map(AccountAmount::closingBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cashChange = closingCash.subtract(openingCash);

        List<String> notes = new ArrayList<>();
        notes.add("Depreciation, impairment and disposal gains or losses are added back from the journals the fixed asset module posted, so they are counted once.");
        if (nonCashFixedAssets.signum() != 0) {
            notes.add("Fixed asset movements that were not paid in cash (bought on credit, already in the ledger, or transferred) are shown under operating activities so they are not reported as cash.");
        }
        return new CashFlowStatementResponse(from, to, netProfit, sections, netCashFlow, openingCash, closingCash,
                cashChange, netCashFlow.subtract(cashChange), cashAccounts, notes);
    }

    /** Splits a journal's investing amount across its cash accounts in proportion to their legs. */
    private static void spread(List<JournalAccountRow> rows, Map<Long, ClassifiedAccount> accounts, BigDecimal cash,
                               BigDecimal investing, Map<Long, BigDecimal> into) {
        BigDecimal left = investing;
        List<JournalAccountRow> legs = rows.stream()
                .filter(r -> accounts.get(r.getAccountId()) != null && accounts.get(r.getAccountId()).accountClass() == AccountClass.CASH_BANK)
                .filter(r -> r.getDebit().subtract(r.getCredit()).signum() == cash.signum())
                .toList();
        for (int i = 0; i < legs.size(); i++) {
            JournalAccountRow r = legs.get(i);
            BigDecimal part = i == legs.size() - 1 ? left
                    : r.getDebit().subtract(r.getCredit()).multiply(investing).divide(cash, 2, java.math.RoundingMode.HALF_UP);
            left = left.subtract(part);
            into.merge(r.getAccountId(), part, BigDecimal::add);
        }
    }

    private static String bucket(AccountClass c) {
        return switch (c) {
            case RECEIVABLE -> "RECEIVABLES";
            case PAYABLE -> "PAYABLES";
            case OTHER_CURRENT_LIABILITY -> "OTHER_CURRENT_LIABILITIES";
            case LOAN_LIABILITY -> "BORROWINGS";
            case NON_CURRENT_LIABILITY -> "NON_CURRENT_LIABILITIES";
            case EQUITY -> "EQUITY";
            default -> "OTHER_CURRENT_ASSETS";
        };
    }

    private Set<Long> mappedIds(MappingKey key) {
        Set<Long> ids = new HashSet<>();
        try {
            accountRepository.findByAccountId(mappingService.resolve(key)).ifPresent(a -> ids.add(a.getId()));
        } catch (RuntimeException ignored) {
            // Not mapped: nothing to treat specially.
        }
        return ids;
    }

    private static List<AccountAmount> cashLines(Map<Long, BigDecimal> amounts, Map<Long, ClassifiedAccount> accounts,
                                                 Map<Long, BigDecimal> opening, Map<Long, BigDecimal> closing) {
        List<AccountAmount> out = new ArrayList<>();
        amounts.forEach((id, amount) -> out.add(amount(accounts.get(id), amount, opening, closing)));
        return out;
    }

    private static AccountAmount amount(ClassifiedAccount a, BigDecimal amount, Map<Long, BigDecimal> opening, Map<Long, BigDecimal> closing) {
        return new AccountAmount(a.id(), a.code(), a.name(), amount,
                opening.getOrDefault(a.id(), BigDecimal.ZERO), closing.getOrDefault(a.id(), BigDecimal.ZERO));
    }

    private static void addLine(List<Line> lines, String key, String label, List<AccountAmount> accounts) {
        if (accounts == null || accounts.isEmpty()) return;
        BigDecimal total = accounts.stream().map(AccountAmount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() == 0) return;
        List<AccountAmount> sorted = new ArrayList<>(accounts);
        sorted.sort((x, y) -> String.valueOf(x.accountCode()).compareTo(String.valueOf(y.accountCode())));
        lines.add(new Line(key, label, total, sorted));
    }

    private static Section section(String key, String title, List<Line> lines) {
        BigDecimal total = lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Section(key, title, lines, total);
    }

    private static Map<Long, BigDecimal> balances(List<AnalyticsBalanceRow> rows) {
        Map<Long, BigDecimal> out = new HashMap<>();
        for (AnalyticsBalanceRow r : rows) out.put(r.getAccountId(), r.getDebit().subtract(r.getCredit()));
        return out;
    }

    private static BigDecimal sum(Map<Long, BigDecimal> m) {
        return m.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
