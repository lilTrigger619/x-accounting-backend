package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.entity.branch.UserAccessScope;
import com.unionsg.xaccounting.exception.AccessDeniedException;
import com.unionsg.xaccounting.repository.branch.BranchRepository;
import com.unionsg.xaccounting.repository.branch.CostCenterRepository;
import com.unionsg.xaccounting.repository.branch.UserAccessScopeRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The one place a user's branch / cost center scope is worked out. Roles say what a user may
 * do; this says where. Journals, approvals, pickers and reports all ask here.
 *
 * <p>No signed-in user (startup seeders, scheduled jobs) means system work, which is Global.
 * A user with no scope rows is limited to the Head Office and all its cost centers, which is
 * also what new users get until an admin sets their scope. Existing users were backfilled to
 * Global when branches were introduced, so nobody lost access.</p>
 */
@Service
@RequiredArgsConstructor
public class AccessScopeService {

    private final UserAccessScopeRepository scopeRepository;
    private final CostCenterRepository costCenterRepository;
    private final BranchRepository branchRepository;

    @Transactional(readOnly = true)
    public AccessScope current() {
        User user = SecurityUtils.getCurrentUser();
        if (user == null) {
            return AccessScope.everything();
        }
        return forUser(user.getId());
    }

    @Transactional(readOnly = true)
    public AccessScope forUser(UUID userId) {
        List<UserAccessScope> rows = scopeRepository.findForUser(userId);
        if (rows.isEmpty()) {
            return headOfficeOnly();
        }
        if (rows.stream().anyMatch(UserAccessScope::isGlobal)) {
            return AccessScope.everything();
        }
        Set<Long> branchIds = new HashSet<>();
        Set<Long> costCenterIds = new HashSet<>();
        Set<Long> wholeBranches = new HashSet<>();
        for (UserAccessScope row : rows) {
            Branch branch = row.getBranch();
            branchIds.add(branch.getId());
            if (Boolean.TRUE.equals(row.getAllCostCenters())) {
                wholeBranches.add(branch.getId());
            } else {
                row.getCostCenters().stream()
                        .filter(cc -> !Boolean.TRUE.equals(cc.getDeleted()))
                        .forEach(cc -> costCenterIds.add(cc.getId()));
            }
        }
        if (!wholeBranches.isEmpty()) {
            costCenterRepository.findByBranchIdInAndDeletedFalse(wholeBranches)
                    .forEach(cc -> costCenterIds.add(cc.getId()));
        }
        return new AccessScope(false, Set.copyOf(branchIds), Set.copyOf(costCenterIds));
    }

    private AccessScope headOfficeOnly() {
        return branchRepository.findFirstByHeadOfficeTrueAndDeletedFalse()
                .map(ho -> new AccessScope(false, Set.of(ho.getId()),
                        Set.copyOf(costCenterRepository.findByBranchIdAndDeletedFalse(ho.getId())
                                .stream().map(CostCenter::getId).toList())))
                .orElse(AccessScope.everything());
    }

    public void assertCanSeeBranch(Long branchId) {
        if (!current().coversBranch(branchId)) {
            throw AccessDeniedException.outOfScope("You don't have access to this branch");
        }
    }

    public void assertCanUseCostCenter(CostCenter costCenter) {
        AccessScope scope = current();
        if (!scope.coversCostCenter(costCenter.getId())) {
            throw AccessDeniedException.outOfScope("You don't have access to cost center " + costCenter.getCode()
                    + " (" + costCenter.getName() + ")");
        }
    }
}
