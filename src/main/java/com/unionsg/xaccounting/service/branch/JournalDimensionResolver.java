package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.branch.UserDimensionDefaultRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Decides which branch and cost center a journal and each of its lines belong to when the
 * caller didn't say. In order: the line's own cost center; the account's home branch (the GL
 * account of a bank account held by a branch); the journal header; the current user's default
 * cost center; the Head Office default cost center. Every module keeps posting unchanged and
 * every line still ends up with a dimension.
 */
@Component
@RequiredArgsConstructor
public class JournalDimensionResolver {

    private final BranchService branchService;
    private final CostCenterService costCenterService;
    private final UserDimensionDefaultRepository userDefaultRepository;
    private final BankAccountRepository bankAccountRepository;

    /** The journal header's cost center from the requested branch / cost center, or the defaults. */
    @Transactional
    public CostCenter headerCostCenter(Long branchId, Long costCenterId) {
        if (costCenterId != null) {
            CostCenter cc = costCenterService.getUsable(costCenterId);
            if (branchId != null && !branchId.equals(cc.getBranch().getId())) {
                throw new BusinessException("Cost center " + cc.getCode() + " belongs to " + cc.getBranch().getName()
                        + ", not to the branch chosen for this journal");
            }
            return cc;
        }
        if (branchId != null) {
            return branchService.defaultCostCenterOf(branchService.getUsable(branchId));
        }
        return currentUserDefault();
    }

    /** The current user's default cost center, falling back to the Head Office's. */
    @Transactional
    public CostCenter currentUserDefault() {
        User user = SecurityUtils.getCurrentUser();
        if (user != null) {
            CostCenter cc = userDefaultRepository.findForUser(user.getId())
                    .map(d -> d.getCostCenter())
                    .filter(c -> Boolean.TRUE.equals(c.getActive()) && !Boolean.TRUE.equals(c.getDeleted())
                            && Boolean.TRUE.equals(c.getBranch().getActive()))
                    .orElse(null);
            if (cc != null) {
                return cc;
            }
        }
        return branchService.ensureHeadOfficeDefaultCostCenter();
    }

    /** A line's cost center: its own, else its account's home branch, else the header's. */
    public CostCenter lineCostCenter(Long requestedCostCenterId, AccountEntity account, CostCenter header,
                                     Map<String, Branch> homeBranches) {
        if (requestedCostCenterId != null) {
            return costCenterService.getUsable(requestedCostCenterId);
        }
        Branch home = account != null ? homeBranches.get(account.getAccountId()) : null;
        if (home != null && !home.getId().equals(header.getBranch().getId()) && Boolean.TRUE.equals(home.getActive())) {
            return branchService.defaultCostCenterOf(home);
        }
        return header;
    }

    /** GL account code -> branch holding the bank account that posts to it. */
    @Transactional(readOnly = true)
    public Map<String, Branch> accountHomeBranches() {
        Map<String, Branch> map = new HashMap<>();
        for (BankAccount bank : bankAccountRepository.findAllWithHomeBranch()) {
            map.putIfAbsent(bank.getGlAccountCode(), bank.getHomeBranch());
        }
        return map;
    }
}
