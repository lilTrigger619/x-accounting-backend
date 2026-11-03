package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.BranchAccountingSettings;
import com.unionsg.xaccounting.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Self-balancing branches. When a journal's lines span branches that don't each balance on
 * their own, adds a pair of lines on the inter-branch account for every out-of-balance branch:
 * one in that branch and its mirror in the hub branch (the journal's own branch), each naming
 * the other as counterparty. A debit balance on the inter-branch account is "due from" the
 * counterparty, a credit is "due to" it. Company-wide the pairs cancel, so consolidated figures
 * are exactly what they'd be without branches.
 */
@Component
@RequiredArgsConstructor
public class InterBranchBalancer {

    private final BranchAccountingSettingsService settingsService;
    private final BranchService branchService;

    public void balance(JournalEntry journal) {
        journal.getLines().removeIf(l -> Boolean.TRUE.equals(l.getInterBranch()));

        BranchAccountingSettings settings = settingsService.settings();
        if (!Boolean.TRUE.equals(settings.getSelfBalancingBranches())) {
            return;
        }

        Map<Long, BigDecimal> net = new LinkedHashMap<>();
        Map<Long, Branch> branches = new LinkedHashMap<>();
        for (JournalLine line : journal.getLines()) {
            Branch branch = line.getBranch();
            if (branch == null) {
                continue;
            }
            branches.putIfAbsent(branch.getId(), branch);
            net.merge(branch.getId(), amount(line.getDebitAmount()).subtract(amount(line.getCreditAmount())), BigDecimal::add);
        }
        if (branches.size() < 2 || net.values().stream().allMatch(v -> v.signum() == 0)) {
            return;
        }

        Branch hub = journal.getBranch() != null && branches.containsKey(journal.getBranch().getId())
                ? journal.getBranch()
                : branches.get(net.entrySet().stream()
                        .max(Comparator.comparing(e -> e.getValue().abs()))
                        .orElseThrow().getKey());

        AccountEntity account = settings.getInterBranchAccount();
        if (account == null) {
            String names = branches.values().stream().map(Branch::getName).collect(Collectors.joining(", "));
            throw new BusinessException("This journal moves value between branches (" + names + "). Choose the "
                    + "inter-branch account in Settings > Branch Accounting before posting across branches.");
        }

        int lineNumber = journal.getLines().stream().mapToInt(l -> l.getLineNumber() == null ? 0 : l.getLineNumber()).max().orElse(0);
        List<JournalLine> added = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> e : net.entrySet()) {
            Branch branch = branches.get(e.getKey());
            BigDecimal n = e.getValue();
            if (branch.getId().equals(hub.getId()) || n.signum() == 0) {
                continue;
            }
            // Branch side: offsets its own imbalance. Debit-heavy (n > 0) means the branch
            // received value from the hub, so it owes the hub: credit inter-branch.
            added.add(line(journal, account, branch, hub, n.signum() < 0 ? n.negate() : BigDecimal.ZERO,
                    n.signum() > 0 ? n : BigDecimal.ZERO, ++lineNumber,
                    (n.signum() > 0 ? "Due to " : "Due from ") + hub.getName()));
            // Hub side: the mirror.
            added.add(line(journal, account, hub, branch, n.signum() > 0 ? n : BigDecimal.ZERO,
                    n.signum() < 0 ? n.negate() : BigDecimal.ZERO, ++lineNumber,
                    (n.signum() > 0 ? "Due from " : "Due to ") + branch.getName()));
        }
        added.forEach(journal::addLine);
    }

    private JournalLine line(JournalEntry journal, AccountEntity account, Branch branch, Branch counterparty,
                             BigDecimal debit, BigDecimal credit, int lineNumber, String description) {
        JournalLine line = new JournalLine();
        line.setAccount(account);
        line.setLineNumber(lineNumber);
        line.setDescription(description);
        line.setDebitAmount(debit);
        line.setCreditAmount(credit);
        line.setCurrencyCode(journal.getCurrencyCode());
        line.setCostCenter(branchService.defaultCostCenterOf(branch));
        line.setBranch(branch);
        line.setCounterpartyBranch(counterparty);
        line.setInterBranch(true);
        return line;
    }

    /** Every branch's lines must net to zero (used by posting when the setting is on). */
    public void assertBalancedPerBranch(JournalEntry journal) {
        if (!Boolean.TRUE.equals(settingsService.settings().getSelfBalancingBranches())) {
            return;
        }
        Map<String, BigDecimal> net = new LinkedHashMap<>();
        for (JournalLine line : journal.getLines()) {
            String key = line.getBranch() != null ? line.getBranch().getName() : "(no branch)";
            net.merge(key, amount(line.getDebitAmount()).subtract(amount(line.getCreditAmount())), BigDecimal::add);
        }
        List<String> off = net.entrySet().stream()
                .filter(e -> e.getValue().signum() != 0)
                .map(e -> e.getKey() + " (" + e.getValue().toPlainString() + ")")
                .toList();
        if (!off.isEmpty()) {
            throw new BusinessException("Journal doesn't balance per branch: " + String.join(", ", off)
                    + ". Each branch's debits must equal its credits.");
        }
    }

    private static BigDecimal amount(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
