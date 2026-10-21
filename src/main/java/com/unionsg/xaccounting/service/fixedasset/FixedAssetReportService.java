package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.MapperLayer.FixedAssetMapper;
import com.unionsg.xaccounting.dto.fixedasset.PeriodDepreciationStatusResponse;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.AccumulatedDepreciationReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.AcquisitionReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.AcquisitionRow;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.AssetBalanceRow;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.BalancesReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.CategoryTotal;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.DashboardResponse;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.DepreciationByPeriodReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.DepreciationMovementRow;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.DisposalReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.DisposalRow;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.JournalHistoryReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.JournalHistoryRow;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.OutsideJournalRow;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.PeriodTotal;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.ReconciliationReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.ReconciliationRow;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.RollForwardRow;
import com.unionsg.xaccounting.dto.reports.BalanceSheetFixedAssetNoteDto;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.entity.fixedasset.AssetTransaction;
import com.unionsg.xaccounting.entity.fixedasset.DepreciationRun;
import com.unionsg.xaccounting.entity.fixedasset.DepreciationRunLine;
import com.unionsg.xaccounting.entity.fixedasset.FixedAsset;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.fixedasset.AssetTransactionType;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationMethod;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.accounting.AccountingPeriodRepository;
import com.unionsg.xaccounting.repository.fixedasset.AssetTransactionRepository;
import com.unionsg.xaccounting.repository.fixedasset.DepreciationRunRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetLedgerRepository;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetRepository;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetAccountRules.Role;
import com.unionsg.xaccounting.service.reports.CurrentFiscalPeriodResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The fixed asset reports. Balances come from the asset subledger ({@code asset_transactions}),
 * which only ever changes together with a posted journal, and the reconciliation report compares
 * it with the general ledger. None of these reports calculates depreciation itself.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixedAssetReportService {

    private static final List<JournalStatus> LEDGER = List.of(JournalStatus.POSTED, JournalStatus.REVERSED);
    private static final Set<AssetTransactionType> DISPOSALS = EnumSet.of(AssetTransactionType.DISPOSAL, AssetTransactionType.RETIREMENT);

    private final FixedAssetRepository assetRepository;
    private final AssetTransactionRepository transactionRepository;
    private final DepreciationRunRepository runRepository;
    private final AccountingPeriodRepository periodRepository;
    private final FixedAssetLedgerRepository ledgerRepository;
    private final AccountRepository accountRepository;
    private final FixedAssetAccountRules rules;
    private final DepreciationRunService runService;
    private final CurrentFiscalPeriodResolver fiscalPeriodResolver;

    public record ReportFilter(Long categoryId, Long locationId, Long departmentId, FixedAssetStatus status) {
        public static ReportFilter none() {
            return new ReportFilter(null, null, null, null);
        }

        boolean matches(FixedAsset a) {
            return (categoryId == null || (a.getCategory() != null && categoryId.equals(a.getCategory().getId())))
                    && (locationId == null || (a.getLocation() != null && locationId.equals(a.getLocation().getId())))
                    && (departmentId == null || (a.getDepartment() != null && departmentId.equals(a.getDepartment().getId())));
        }
    }

    // ------------------------------------------------------------------
    // Register and net book value
    // ------------------------------------------------------------------

    /** Every asset on the books (or disposed of) on {@code asOf}, with its balances that day. */
    public BalancesReport balances(LocalDate asOf, ReportFilter filter) {
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        Map<Long, Totals> totals = totalsUpTo(date);
        List<AssetBalanceRow> rows = new ArrayList<>();
        for (FixedAsset a : assetRepository.findByDeletedFalseOrderByAssetNumberAsc()) {
            Totals t = totals.get(a.getId());
            if (t == null || !filter.matches(a)) continue;
            String status = statusOn(a, t, date);
            if (filter.status() != null && !filter.status().name().equals(status)) continue;
            rows.add(new AssetBalanceRow(a.getId(), a.getAssetNumber(), a.getName(),
                    a.getCategory() != null ? a.getCategory().getId() : null,
                    a.getCategory() != null ? a.getCategory().getName() : null,
                    a.getLocation() != null ? a.getLocation().getName() : null,
                    a.getDepartment() != null ? a.getDepartment().getName() : null,
                    a.getAcquisitionDate(), a.getAvailableForUseDate(),
                    a.getDepreciationMethod() != null ? a.getDepreciationMethod().name() : null, status,
                    t.cost, t.depreciation, t.impairment, t.carrying(), a.getResidualValue(), a.getUsefulLifeMonths()));
        }
        Map<String, List<AssetBalanceRow>> byCategory = rows.stream().collect(Collectors.groupingBy(
                r -> r.categoryName() != null ? r.categoryName() : "Uncategorised", TreeMap::new, Collectors.toList()));
        List<CategoryTotal> categories = new ArrayList<>();
        byCategory.forEach((name, list) -> categories.add(categoryTotal(list.get(0).categoryId(), name, list)));
        return new BalancesReport(date, rows, categories, categoryTotal(null, "Total", rows));
    }

    private static CategoryTotal categoryTotal(Long id, String name, List<AssetBalanceRow> rows) {
        return new CategoryTotal(id, name, rows.size(),
                sum(rows, AssetBalanceRow::cost), sum(rows, AssetBalanceRow::accumulatedDepreciation),
                sum(rows, AssetBalanceRow::accumulatedImpairment), sum(rows, AssetBalanceRow::carryingAmount));
    }

    private static String statusOn(FixedAsset a, Totals t, LocalDate date) {
        if (t.cost.signum() == 0 && a.getDisposalDate() != null && !a.getDisposalDate().isAfter(date)) {
            return a.getStatus().name();
        }
        if (a.getDepreciationMethod() != DepreciationMethod.NOT_DEPRECIATED && t.cost.signum() > 0
                && t.carrying().compareTo(nz(a.getResidualValue())) <= 0) {
            return FixedAssetStatus.FULLY_DEPRECIATED.name();
        }
        return FixedAssetStatus.ACTIVE.name();
    }

    // ------------------------------------------------------------------
    // Depreciation by period and accumulated depreciation
    // ------------------------------------------------------------------

    public DepreciationByPeriodReport depreciationByPeriod(LocalDate from, LocalDate to, ReportFilter filter) {
        requireRange(from, to);
        List<DepreciationMovementRow> rows = new ArrayList<>();
        Map<YearMonth, Map<String, BigDecimal>> byMonth = new TreeMap<>();
        for (AssetTransaction t : transactionRepository.findBetween(from, to)) {
            if (!isDepreciation(t) || !filter.matches(t.getAsset())) continue;
            rows.add(movement(t));
            String category = categoryName(t.getAsset());
            byMonth.computeIfAbsent(YearMonth.from(t.getTransactionDate()), k -> new TreeMap<>())
                    .merge(category, t.getDepreciationAmount(), BigDecimal::add);
        }
        List<PeriodTotal> periods = new ArrayList<>();
        byMonth.forEach((month, categories) -> {
            AccountingPeriod p = periodRepository.findByDateInRange(month.atDay(1)).orElse(null);
            BigDecimal amount = categories.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            periods.add(new PeriodTotal(p != null ? p.getName() : month.toString(),
                    p != null ? p.getStartDate() : month.atDay(1), p != null ? p.getEndDate() : month.atEndOfMonth(),
                    amount, categories));
        });
        return new DepreciationByPeriodReport(from, to, periods, rows, sum(rows, DepreciationMovementRow::amount));
    }

    public AccumulatedDepreciationReport accumulatedDepreciation(LocalDate from, LocalDate to, ReportFilter filter) {
        requireRange(from, to);
        Map<Long, Totals> opening = totalsUpTo(from.minusDays(1));
        Map<Long, RollForward> moves = new LinkedHashMap<>();
        for (AssetTransaction t : transactionRepository.findBetween(from, to)) {
            RollForward r = moves.computeIfAbsent(t.getAsset().getId(), k -> new RollForward());
            AssetTransactionType kind = kind(t);
            BigDecimal amount = t.getDepreciationAmount();
            if (kind == AssetTransactionType.DEPRECIATION || kind == AssetTransactionType.ACQUISITION) {
                if (amount.signum() >= 0) r.charge = r.charge.add(amount);
                else r.reversals = r.reversals.add(amount.negate());
            } else if (DISPOSALS.contains(kind)) {
                r.disposals = r.disposals.add(amount.negate());
            }
        }
        List<RollForwardRow> rows = new ArrayList<>();
        for (FixedAsset a : assetRepository.findByDeletedFalseOrderByAssetNumberAsc()) {
            if (!filter.matches(a)) continue;
            Totals o = opening.get(a.getId());
            RollForward m = moves.get(a.getId());
            if (o == null && m == null) continue;
            BigDecimal open = o != null ? o.depreciation : BigDecimal.ZERO;
            RollForward mv = m != null ? m : new RollForward();
            BigDecimal close = open.add(mv.charge).subtract(mv.reversals).subtract(mv.disposals);
            if (open.signum() == 0 && close.signum() == 0 && mv.isEmpty()) continue;
            rows.add(new RollForwardRow(a.getId(), a.getAssetNumber(), a.getName(), categoryName(a), open, mv.charge,
                    mv.reversals, BigDecimal.ZERO, mv.disposals, close));
        }
        Map<String, List<RollForwardRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(RollForwardRow::categoryName, TreeMap::new, Collectors.toList()));
        List<RollForwardRow> byCategory = new ArrayList<>();
        grouped.forEach((name, list) -> byCategory.add(rollUp(name, list)));
        return new AccumulatedDepreciationReport(from, to, rows, byCategory, rollUp("Total", rows));
    }

    private static RollForwardRow rollUp(String name, List<RollForwardRow> rows) {
        return new RollForwardRow(null, null, name, name, sum(rows, RollForwardRow::opening), sum(rows, RollForwardRow::charge),
                sum(rows, RollForwardRow::reversals), BigDecimal.ZERO, sum(rows, RollForwardRow::disposals),
                sum(rows, RollForwardRow::closing));
    }

    // ------------------------------------------------------------------
    // Acquisitions and disposals
    // ------------------------------------------------------------------

    public AcquisitionReport acquisitions(LocalDate from, LocalDate to, ReportFilter filter) {
        requireRange(from, to);
        List<AcquisitionRow> rows = new ArrayList<>();
        for (AssetTransaction t : transactionRepository.findBetween(from, to)) {
            if (t.getType() != AssetTransactionType.ACQUISITION || !filter.matches(t.getAsset())) continue;
            FixedAsset a = t.getAsset();
            rows.add(new AcquisitionRow(t.getId(), a.getId(), a.getAssetNumber(), a.getName(), categoryName(a),
                    t.getTransactionDate(), t.getCostAmount(), t.getDepreciationAmount(),
                    a.getAcquisitionSource() != null ? a.getAcquisitionSource().name() : null,
                    t.getJournal() != null ? t.getJournal().getId() : null,
                    t.getJournal() != null ? t.getJournal().getJournalNumber() : null, t.isReversed()));
        }
        BigDecimal total = rows.stream().filter(r -> !r.reversed()).map(AcquisitionRow::cost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AcquisitionReport(from, to, rows, total);
    }

    public DisposalReport disposals(LocalDate from, LocalDate to, ReportFilter filter) {
        requireRange(from, to);
        List<DisposalRow> rows = new ArrayList<>();
        for (AssetTransaction t : transactionRepository.findBetween(from, to)) {
            if (!DISPOSALS.contains(t.getType()) || !filter.matches(t.getAsset())) continue;
            FixedAsset a = t.getAsset();
            BigDecimal cost = t.getCostAmount().negate();
            BigDecimal dep = t.getDepreciationAmount().negate();
            BigDecimal imp = t.getImpairmentAmount().negate();
            rows.add(new DisposalRow(t.getId(), a.getId(), a.getAssetNumber(), a.getName(), categoryName(a),
                    t.getTransactionDate(), t.getType() == AssetTransactionType.RETIREMENT ? "RETIREMENT"
                    : a.getDisposalType() != null ? a.getDisposalType().name() : t.getType().name(),
                    cost, dep, imp, cost.subtract(dep).subtract(imp), nz(t.getProceedsAmount()), nz(t.getGainLossAmount()),
                    t.getJournal() != null ? t.getJournal().getId() : null,
                    t.getJournal() != null ? t.getJournal().getJournalNumber() : null, t.isReversed()));
        }
        List<DisposalRow> live = rows.stream().filter(r -> !r.reversed()).toList();
        return new DisposalReport(from, to, rows, sum(live, DisposalRow::cost), sum(live, DisposalRow::carryingAmount),
                sum(live, DisposalRow::proceeds), sum(live, DisposalRow::gainLoss));
    }

    // ------------------------------------------------------------------
    // Reconciliation and journal history
    // ------------------------------------------------------------------

    /**
     * Compares the register with the general ledger, account by account, on {@code asOf}, and lists
     * journals from outside the module that moved those accounts since {@code from}.
     */
    public ReconciliationReport reconciliation(LocalDate from, LocalDate asOf) {
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        LocalDate since = from != null ? from : fiscalPeriodResolver.resolveYearStart(date);
        Map<Long, BigDecimal> subledger = new HashMap<>();
        Map<Long, Set<String>> roles = new HashMap<>();
        for (AssetTransaction t : transactionRepository.findUpTo(date)) {
            FixedAsset a = t.getAsset();
            put(subledger, roles, a.getCostAccount(), t.getCostAmount(), Role.COST);
            put(subledger, roles, a.getAccumulatedDepreciationAccount(), t.getDepreciationAmount().negate(), Role.ACCUMULATED_DEPRECIATION);
            put(subledger, roles, a.getAccumulatedImpairmentAccount(), t.getImpairmentAmount().negate(), Role.ACCUMULATED_IMPAIRMENT);
        }
        Map<Role, Set<Long>> configured = rules.configuredAccountIds();
        for (Role role : List.of(Role.COST, Role.ACCUMULATED_DEPRECIATION, Role.ACCUMULATED_IMPAIRMENT)) {
            for (Long id : configured.get(role)) {
                subledger.putIfAbsent(id, BigDecimal.ZERO);
                roles.computeIfAbsent(id, k -> new java.util.TreeSet<>()).add(role.getLabel());
            }
        }
        Map<Long, BigDecimal> ledger = subledger.isEmpty() ? Map.of()
                : ledgerRepository.balancesAsOf(subledger.keySet(), date, LEDGER).stream()
                .collect(Collectors.toMap(FixedAssetLedgerRepository.AccountBalance::getAccountId,
                        FixedAssetLedgerRepository.AccountBalance::getBalance));
        Map<Long, AccountEntity> accounts = accountRepository.findAllById(subledger.keySet()).stream()
                .collect(Collectors.toMap(AccountEntity::getId, Function.identity()));
        List<ReconciliationRow> rows = new ArrayList<>();
        for (Long id : subledger.keySet()) {
            AccountEntity acc = accounts.get(id);
            if (acc == null) continue;
            BigDecimal sub = subledger.get(id);
            BigDecimal gl = ledger.getOrDefault(id, BigDecimal.ZERO);
            rows.add(new ReconciliationRow(id, acc.getAccountId(), acc.getAccountName(), String.join(", ", roles.get(id)),
                    sub, gl, gl.subtract(sub)));
        }
        rows.sort(Comparator.comparing(ReconciliationRow::accountCode));
        BigDecimal subTotal = sum(rows, ReconciliationRow::subledgerBalance);
        BigDecimal glTotal = sum(rows, ReconciliationRow::ledgerBalance);
        BigDecimal difference = rows.stream().map(r -> r.difference().abs()).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OutsideJournalRow> outside = new ArrayList<>();
        Set<Long> sourceJournals = new HashSet<>();
        for (FixedAsset a : assetRepository.findByDeletedFalseOrderByAssetNumberAsc()) {
            if (a.getSourceJournal() != null) sourceJournals.add(a.getSourceJournal().getId());
        }
        if (!subledger.isEmpty() && !since.isAfter(date)) {
            for (JournalLine l : ledgerRepository.outsideLines(subledger.keySet(), since, date, LEDGER, DepreciationRunService.MODULE_SOURCES)) {
                JournalEntry j = l.getJournalEntry();
                if (sourceJournals.contains(j.getId())) continue;
                outside.add(new OutsideJournalRow(j.getId(), j.getJournalNumber(), j.getJournalDate(), j.getDescription(),
                        j.getSourceModule(), l.getAccount().getId(), l.getAccount().getAccountId(), l.getAccount().getAccountName(),
                        l.getDebitAmount(), l.getCreditAmount()));
            }
        }
        return new ReconciliationReport(since, date, rows, subTotal, glTotal, difference, difference.signum() == 0, outside);
    }

    private static void put(Map<Long, BigDecimal> balances, Map<Long, Set<String>> roles, AccountEntity account,
                            BigDecimal amount, Role role) {
        if (account == null) return;
        balances.merge(account.getId(), amount, BigDecimal::add);
        roles.computeIfAbsent(account.getId(), k -> new java.util.TreeSet<>()).add(role.getLabel());
    }

    public JournalHistoryReport journalHistory(LocalDate from, LocalDate to, Long assetId) {
        requireRange(from, to);
        List<JournalEntry> journals = ledgerRepository.moduleJournals(from, to, DepreciationRunService.MODULE_SOURCES);
        Set<Long> assetIds = new HashSet<>();
        Set<Long> runIds = new HashSet<>();
        for (JournalEntry j : journals) {
            if (FixedAssetJournalService.SOURCE_ASSET.equals(j.getSourceModule())) assetIds.add(j.getSourceEntityId());
            else runIds.add(j.getSourceEntityId());
        }
        Map<Long, String> assetNumbers = assetRepository.findAllById(assetIds).stream()
                .collect(Collectors.toMap(FixedAsset::getId, FixedAsset::getAssetNumber));
        Map<Long, DepreciationRun> runs = runRepository.findAllById(runIds).stream()
                .collect(Collectors.toMap(DepreciationRun::getId, Function.identity()));
        List<JournalHistoryRow> rows = new ArrayList<>();
        for (JournalEntry j : journals) {
            boolean run = FixedAssetJournalService.SOURCE_RUN.equals(j.getSourceModule());
            if (assetId != null) {
                boolean touches = run
                        ? runs.containsKey(j.getSourceEntityId()) && runs.get(j.getSourceEntityId()).getLines().stream()
                        .anyMatch(l -> l.getAsset().getId().equals(assetId))
                        : assetId.equals(j.getSourceEntityId());
                if (!touches) continue;
            }
            rows.add(new JournalHistoryRow(j.getId(), j.getJournalNumber(), j.getJournalDate(),
                    j.getJournalType() != null ? j.getJournalType().name() : null, j.getStatus().name(), j.getDescription(),
                    j.getTotalDebit(), j.getSourceModule(), j.getSourceEntityId(),
                    run ? (runs.containsKey(j.getSourceEntityId()) ? runs.get(j.getSourceEntityId()).getRunNumber() : null)
                            : assetNumbers.get(j.getSourceEntityId()),
                    j.getReversalOfJournalId()));
        }
        return new JournalHistoryReport(from, to, rows);
    }

    // ------------------------------------------------------------------
    // Dashboard and Balance Sheet note
    // ------------------------------------------------------------------

    public DashboardResponse dashboard() {
        LocalDate today = LocalDate.now();
        BalancesReport balances = balances(today, ReportFilter.none());
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (FixedAssetStatus s : FixedAssetStatus.values()) byStatus.put(s.name(), 0L);
        long accountProblems = 0;
        for (FixedAsset a : assetRepository.findByDeletedFalseOrderByAssetNumberAsc()) {
            byStatus.merge(a.getStatus().name(), 1L, Long::sum);
            if (a.getStatus() == FixedAssetStatus.DRAFT || FixedAssetService.ON_BOOKS.contains(a.getStatus())) {
                for (Role role : List.of(Role.COST, Role.ACCUMULATED_DEPRECIATION, Role.DEPRECIATION_EXPENSE)) {
                    try {
                        rules.resolve(a, role);
                    } catch (BusinessException e) {
                        accountProblems++;
                        break;
                    }
                }
            }
        }
        PeriodDepreciationStatusResponse current = null;
        AccountingPeriod period = periodRepository.findByDateInRange(today).orElse(null);
        LocalDate periodStart = period != null ? period.getStartDate() : today.withDayOfMonth(1);
        LocalDate periodEnd = period != null ? period.getEndDate() : YearMonth.from(today).atEndOfMonth();
        if (period != null) {
            current = runService.periods().stream().filter(p -> p.getPeriodId().equals(period.getId())).findFirst().orElse(null);
        }
        LocalDate yearStart = fiscalPeriodResolver.resolveYearStart(today);
        BigDecimal thisPeriod = BigDecimal.ZERO;
        BigDecimal ytd = BigDecimal.ZERO;
        for (AssetTransaction t : transactionRepository.findBetween(yearStart.isBefore(periodStart) ? yearStart : periodStart, periodEnd)) {
            if (!isDepreciation(t)) continue;
            if (!t.getTransactionDate().isBefore(yearStart)) ytd = ytd.add(t.getDepreciationAmount());
            if (!t.getTransactionDate().isBefore(periodStart)) thisPeriod = thisPeriod.add(t.getDepreciationAmount());
        }
        long awaiting = runRepository.findByStatusInOrderByPeriodStartDesc(EnumSet.of(
                DepreciationRunStatus.CALCULATED, DepreciationRunStatus.SUBMITTED, DepreciationRunStatus.APPROVED)).size();
        BigDecimal difference;
        try {
            difference = reconciliation(today, today).difference();
        } catch (RuntimeException e) {
            difference = null;
        }
        return new DashboardResponse(today, byStatus, balances.total(), balances.byCategory(), thisPeriod, ytd, current,
                runRepository.findTop10ByDeletedFalseOrderByIdDesc().stream().map(FixedAssetMapper::toRunListItem).toList(),
                awaiting, difference, accountProblems);
    }

    /** The PP&E note for the Balance Sheet on {@code asOf}, from the same GL balances. */
    public BalanceSheetFixedAssetNoteDto balanceSheetNote(LocalDate asOf) {
        Map<Role, Set<Long>> configured = rules.configuredAccountIds();
        Set<Long> ids = new HashSet<>();
        configured.get(Role.COST).forEach(ids::add);
        configured.get(Role.ACCUMULATED_DEPRECIATION).forEach(ids::add);
        configured.get(Role.ACCUMULATED_IMPAIRMENT).forEach(ids::add);
        if (ids.isEmpty()) return null;
        Map<Long, BigDecimal> gl = ledgerRepository.balancesAsOf(ids, asOf, LEDGER).stream()
                .collect(Collectors.toMap(FixedAssetLedgerRepository.AccountBalance::getAccountId,
                        FixedAssetLedgerRepository.AccountBalance::getBalance));
        Map<Long, AccountEntity> accounts = accountRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(AccountEntity::getId, Function.identity()));
        Set<Long> contra = new HashSet<>(configured.get(Role.ACCUMULATED_DEPRECIATION));
        contra.addAll(configured.get(Role.ACCUMULATED_IMPAIRMENT));
        List<BalanceSheetFixedAssetNoteDto.Line> cost = noteLines(configured.get(Role.COST), contra, accounts, gl, false);
        List<BalanceSheetFixedAssetNoteDto.Line> dep = noteLines(configured.get(Role.ACCUMULATED_DEPRECIATION), Set.of(), accounts, gl, true);
        Set<Long> impairmentOnly = new HashSet<>(configured.get(Role.ACCUMULATED_IMPAIRMENT));
        impairmentOnly.removeAll(configured.get(Role.ACCUMULATED_DEPRECIATION));
        List<BalanceSheetFixedAssetNoteDto.Line> imp = noteLines(impairmentOnly, Set.of(), accounts, gl, true);
        BigDecimal totalCost = cost.stream().map(BalanceSheetFixedAssetNoteDto.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDep = dep.stream().map(BalanceSheetFixedAssetNoteDto.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalImp = imp.stream().map(BalanceSheetFixedAssetNoteDto.Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalCost.signum() == 0 && totalDep.signum() == 0 && totalImp.signum() == 0) return null;
        BigDecimal net = totalCost.subtract(totalDep).subtract(totalImp);
        BigDecimal subledger = balances(asOf, ReportFilter.none()).total().carryingAmount();
        return new BalanceSheetFixedAssetNoteDto(cost, totalCost, dep, totalDep, imp, totalImp, net, subledger, net.subtract(subledger));
    }

    private static List<BalanceSheetFixedAssetNoteDto.Line> noteLines(Set<Long> ids, Set<Long> exclude, Map<Long, AccountEntity> accounts,
                                                                      Map<Long, BigDecimal> gl, boolean contra) {
        List<BalanceSheetFixedAssetNoteDto.Line> out = new ArrayList<>();
        for (Long id : ids) {
            if (exclude.contains(id)) continue;
            AccountEntity a = accounts.get(id);
            BigDecimal balance = gl.getOrDefault(id, BigDecimal.ZERO);
            if (a == null || balance.signum() == 0) continue;
            out.add(new BalanceSheetFixedAssetNoteDto.Line(id, a.getAccountId(), a.getAccountName(), contra ? balance.negate() : balance));
        }
        out.sort(Comparator.comparing(BalanceSheetFixedAssetNoteDto.Line::accountCode));
        return out;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Map<Long, Totals> totalsUpTo(LocalDate date) {
        Map<Long, Totals> out = new HashMap<>();
        for (AssetTransaction t : transactionRepository.findUpTo(date)) {
            Totals x = out.computeIfAbsent(t.getAsset().getId(), k -> new Totals());
            x.cost = x.cost.add(t.getCostAmount());
            x.depreciation = x.depreciation.add(t.getDepreciationAmount());
            x.impairment = x.impairment.add(t.getImpairmentAmount());
        }
        return out;
    }

    private static DepreciationMovementRow movement(AssetTransaction t) {
        FixedAsset a = t.getAsset();
        DepreciationRunLine line = t.getRunLine() != null ? t.getRunLine() : t.getReverses() != null ? t.getReverses().getRunLine() : null;
        DepreciationRun run = line != null ? line.getRun() : null;
        return new DepreciationMovementRow(t.getId(), a.getId(), a.getAssetNumber(), a.getName(), categoryName(a),
                t.getTransactionDate(), t.getType().name(), t.getDepreciationAmount(),
                t.getJournal() != null ? t.getJournal().getId() : null, t.getJournal() != null ? t.getJournal().getJournalNumber() : null,
                run != null ? run.getId() : null, run != null ? run.getRunNumber() : null,
                run != null ? run.getAccountingPeriod().getName() : null, t.getDescription());
    }

    private static boolean isDepreciation(AssetTransaction t) {
        return kind(t) == AssetTransactionType.DEPRECIATION;
    }

    private static AssetTransactionType kind(AssetTransaction t) {
        return t.getType() == AssetTransactionType.REVERSAL ? t.getMovementType() : t.getType();
    }

    private static String categoryName(FixedAsset a) {
        return a.getCategory() != null ? a.getCategory().getName() : "Uncategorised";
    }

    private static void requireRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new BadRequestException("A from and a to date are required");
        if (from.isAfter(to)) throw new BadRequestException("The from date can't be after the to date");
    }

    private static <T> BigDecimal sum(List<T> rows, Function<T, BigDecimal> f) {
        return rows.stream().map(f).map(FixedAssetReportService::nz).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static final class Totals {
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal depreciation = BigDecimal.ZERO;
        BigDecimal impairment = BigDecimal.ZERO;

        BigDecimal carrying() {
            return cost.subtract(depreciation).subtract(impairment);
        }
    }

    private static final class RollForward {
        BigDecimal charge = BigDecimal.ZERO;
        BigDecimal reversals = BigDecimal.ZERO;
        BigDecimal disposals = BigDecimal.ZERO;

        boolean isEmpty() {
            return charge.signum() == 0 && reversals.signum() == 0 && disposals.signum() == 0;
        }
    }
}
