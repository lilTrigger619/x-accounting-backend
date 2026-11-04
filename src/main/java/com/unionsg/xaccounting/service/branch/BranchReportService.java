package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.dto.branch.CostCenterPnlResponse;
import com.unionsg.xaccounting.dto.branch.CostCenterResponse;
import com.unionsg.xaccounting.dto.branch.InterBranchReconciliationResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.branch.BranchLedgerRepository;
import com.unionsg.xaccounting.repository.branch.BranchRepository;
import com.unionsg.xaccounting.repository.branch.CostCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchReportService {

    private final BranchLedgerRepository ledgerRepository;
    private final BranchRepository branchRepository;
    private final CostCenterRepository costCenterRepository;
    private final BranchAccountingSettingsService settingsService;
    private final AccessScopeService accessScopeService;
    private final ReportScopeResolver reportScopeResolver;

    /**
     * Each branch pair's balances against each other on the inter-branch account. A's balance
     * with B and B's balance with A must be equal and opposite; any difference means one side
     * was posted by hand. Pairs are limited to branches in the user's scope.
     */
    @Transactional(readOnly = true)
    public InterBranchReconciliationResponse interBranchReconciliation(LocalDate asOf) {
        AccountEntity account = settingsService.settings().getInterBranchAccount();
        if (account == null) {
            throw new BusinessException("Choose the inter-branch account in Settings > Branch Accounting first");
        }
        AccessScope scope = accessScopeService.current();
        Map<Long, Branch> branches = branchRepository.findAll().stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity()));

        Map<String, BigDecimal> balance = new HashMap<>();
        BigDecimal consolidated = BigDecimal.ZERO;
        Set<Long> involved = new HashSet<>();
        for (BranchLedgerRepository.InterBranchBalance row : ledgerRepository.interBranchBalances(account.getId(), asOf)) {
            BigDecimal net = row.getNet() == null ? BigDecimal.ZERO : row.getNet();
            consolidated = consolidated.add(net);
            balance.merge(key(row.getBranchId(), row.getCounterpartyBranchId()), net, BigDecimal::add);
            if (row.getBranchId() != null) involved.add(row.getBranchId());
            if (row.getCounterpartyBranchId() != null) involved.add(row.getCounterpartyBranchId());
        }

        List<InterBranchReconciliationResponse.Pair> pairs = new ArrayList<>();
        List<Long> ids = involved.stream().sorted().toList();
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                Long a = ids.get(i);
                Long b = ids.get(j);
                if (!scope.coversBranch(a) && !scope.coversBranch(b)) {
                    continue;
                }
                BigDecimal ab = balance.getOrDefault(key(a, b), BigDecimal.ZERO);
                BigDecimal ba = balance.getOrDefault(key(b, a), BigDecimal.ZERO);
                if (ab.signum() == 0 && ba.signum() == 0) {
                    continue;
                }
                BigDecimal diff = ab.add(ba);
                pairs.add(InterBranchReconciliationResponse.Pair.builder()
                        .branchId(a).branchCode(code(branches, a)).branchName(name(branches, a))
                        .counterpartyBranchId(b).counterpartyBranchCode(code(branches, b)).counterpartyBranchName(name(branches, b))
                        .branchBalance(ab).counterpartyBalance(ba).difference(diff)
                        .reconciled(diff.signum() == 0)
                        .build());
            }
        }
        // Lines on the account with no counterparty were posted by hand and can't be matched.
        for (Long id : ids) {
            BigDecimal orphan = balance.getOrDefault(key(id, null), BigDecimal.ZERO);
            if (orphan.signum() != 0 && scope.coversBranch(id)) {
                pairs.add(InterBranchReconciliationResponse.Pair.builder()
                        .branchId(id).branchCode(code(branches, id)).branchName(name(branches, id))
                        .counterpartyBranchName("(no counterparty)")
                        .branchBalance(orphan).counterpartyBalance(BigDecimal.ZERO).difference(orphan)
                        .reconciled(false)
                        .build());
            }
        }
        pairs.sort(Comparator.comparing(InterBranchReconciliationResponse.Pair::isReconciled)
                .thenComparing(p -> p.getBranchName() == null ? "" : p.getBranchName()));
        return InterBranchReconciliationResponse.builder()
                .asOfDate(asOf)
                .interBranchAccountCode(account.getAccountId())
                .interBranchAccountName(account.getAccountName())
                .pairs(pairs)
                .consolidatedNet(consolidated)
                .reconciled(consolidated.signum() == 0 && pairs.stream().allMatch(InterBranchReconciliationResponse.Pair::isReconciled))
                .build();
    }

    /** Income and expense per account, one column per cost center. */
    @Transactional(readOnly = true)
    public CostCenterPnlResponse costCenterPnl(LocalDate from, LocalDate to, List<Long> branchIds, List<Long> costCenterIds) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("Choose a valid period: the start date must be on or before the end date");
        }
        Set<Long> filter = reportScopeResolver.costCenterFilter(branchIds, costCenterIds);
        List<CostCenterResponse> columns = costCenterRepository.search(null, false, null).stream()
                .filter(cc -> filter == null || filter.contains(cc.getId()))
                .map(BranchMapper::toResponse)
                .toList();
        Set<Long> ids = columns.stream().map(CostCenterResponse::getId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            ids = Set.of(-1L);
        }

        Map<Long, CostCenterPnlResponse.Row> rows = new LinkedHashMap<>();
        Map<Long, BigDecimal> net = new HashMap<>();
        BigDecimal netTotal = BigDecimal.ZERO;
        for (BranchLedgerRepository.CostCenterActivity a : ledgerRepository.costCenterActivity(from, to, ids, false)) {
            boolean income = a.getAccountType() == AccountType.INCOME;
            BigDecimal amount = income ? a.getTotalCredit().subtract(a.getTotalDebit()) : a.getTotalDebit().subtract(a.getTotalCredit());
            CostCenterPnlResponse.Row row = rows.computeIfAbsent(a.getAccountId(), k -> CostCenterPnlResponse.Row.builder()
                    .accountId(a.getAccountId()).accountCode(a.getAccountCode()).accountName(a.getAccountName())
                    .accountType(a.getAccountType().name()).amounts(new HashMap<>()).total(BigDecimal.ZERO).build());
            row.getAmounts().merge(a.getCostCenterId(), amount, BigDecimal::add);
            row.setTotal(row.getTotal().add(amount));
            BigDecimal signed = income ? amount : amount.negate();
            net.merge(a.getCostCenterId(), signed, BigDecimal::add);
            netTotal = netTotal.add(signed);
        }
        List<CostCenterPnlResponse.Row> sorted = rows.values().stream()
                .sorted(Comparator.comparing((CostCenterPnlResponse.Row r) -> !"INCOME".equals(r.getAccountType()))
                        .thenComparing(CostCenterPnlResponse.Row::getAccountCode))
                .toList();
        return CostCenterPnlResponse.builder()
                .fromDate(from).toDate(to)
                .costCenters(columns)
                .rows(sorted)
                .netByCostCenter(net)
                .netTotal(netTotal)
                .build();
    }

    private static String key(Long a, Long b) {
        return a + ":" + b;
    }

    private static String code(Map<Long, Branch> branches, Long id) {
        Branch b = branches.get(id);
        return b != null ? b.getCode() : null;
    }

    private static String name(Map<Long, Branch> branches, Long id) {
        Branch b = branches.get(id);
        return b != null ? b.getName() : "(unknown branch)";
    }
}
