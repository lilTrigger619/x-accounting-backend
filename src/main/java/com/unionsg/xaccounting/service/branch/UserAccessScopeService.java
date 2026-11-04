package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.dto.branch.BranchResponse;
import com.unionsg.xaccounting.dto.branch.CostCenterResponse;
import com.unionsg.xaccounting.dto.branch.MyScopeResponse;
import com.unionsg.xaccounting.dto.branch.UserAccessScopeRequest;
import com.unionsg.xaccounting.dto.branch.UserAccessScopeResponse;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.entity.branch.UserAccessScope;
import com.unionsg.xaccounting.entity.branch.UserDimensionDefault;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.UserRepository;
import com.unionsg.xaccounting.repository.branch.UserAccessScopeRepository;
import com.unionsg.xaccounting.repository.branch.UserDimensionDefaultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Reads and replaces a user's branch / cost center scope and default cost center. */
@Service
@RequiredArgsConstructor
public class UserAccessScopeService {

    private final UserAccessScopeRepository scopeRepository;
    private final UserDimensionDefaultRepository defaultRepository;
    private final UserRepository userRepository;
    private final BranchService branchService;
    private final CostCenterService costCenterService;
    private final AccessScopeService accessScopeService;
    private final BranchAccountingSettingsService settingsService;
    private final JournalDimensionResolver dimensionResolver;

    @Transactional(readOnly = true)
    public UserAccessScopeResponse get(UUID userId) {
        User user = user(userId);
        List<UserAccessScope> rows = scopeRepository.findForUser(user.getId());
        boolean global = rows.stream().anyMatch(UserAccessScope::isGlobal);
        List<UserAccessScopeResponse.BranchScope> branches = global ? List.of() : rows.stream()
                .sorted(Comparator.comparing(r -> r.getBranch().getName()))
                .map(r -> UserAccessScopeResponse.BranchScope.builder()
                        .branchId(r.getBranch().getId())
                        .branchCode(r.getBranch().getCode())
                        .branchName(r.getBranch().getName())
                        .allCostCenters(Boolean.TRUE.equals(r.getAllCostCenters()))
                        .costCenters(Boolean.TRUE.equals(r.getAllCostCenters()) ? List.of() : r.getCostCenters().stream()
                                .filter(cc -> !Boolean.TRUE.equals(cc.getDeleted()))
                                .sorted(Comparator.comparing(CostCenter::getName))
                                .map(BranchMapper::toResponse).toList())
                        .build())
                .toList();
        CostCenter def = defaultRepository.findForUser(user.getId()).map(UserDimensionDefault::getCostCenter).orElse(null);
        return UserAccessScopeResponse.builder()
                .userId(user.getId())
                .global(global)
                .branches(branches)
                .defaultCostCenterId(def != null ? def.getId() : null)
                .defaultCostCenterCode(def != null ? def.getCode() : null)
                .defaultCostCenterName(def != null ? def.getName() : null)
                .defaultBranchName(def != null ? def.getBranch().getName() : null)
                .summary(summary(rows, global))
                .build();
    }

    @Transactional
    public UserAccessScopeResponse replace(UUID userId, UserAccessScopeRequest request) {
        User user = user(userId);
        boolean global = Boolean.TRUE.equals(request.getGlobal());
        List<UserAccessScopeRequest.BranchScope> entries = request.getBranches() == null ? List.of() : request.getBranches();
        if (!global && entries.isEmpty()) {
            throw new BusinessException("Choose Global, or at least one branch this user works in");
        }

        List<UserAccessScope> rows = new ArrayList<>();
        Set<Long> coveredCostCenters = new HashSet<>();
        Set<Long> wholeBranches = new HashSet<>();
        if (global) {
            UserAccessScope row = new UserAccessScope();
            row.setUser(user);
            row.setAllCostCenters(true);
            rows.add(row);
        } else {
            Set<Long> seen = new HashSet<>();
            for (UserAccessScopeRequest.BranchScope entry : entries) {
                Branch branch = branchService.getEntity(entry.getBranchId());
                if (!seen.add(branch.getId())) {
                    throw new BusinessException("\"" + branch.getName() + "\" is listed twice");
                }
                UserAccessScope row = new UserAccessScope();
                row.setUser(user);
                row.setBranch(branch);
                boolean all = entry.getAllCostCenters() == null || entry.getAllCostCenters();
                row.setAllCostCenters(all);
                if (all) {
                    wholeBranches.add(branch.getId());
                } else {
                    List<Long> ids = entry.getCostCenterIds() == null ? List.of() : entry.getCostCenterIds();
                    if (ids.isEmpty()) {
                        throw new BusinessException("Tick at least one cost center in \"" + branch.getName()
                                + "\", or give the user all of its cost centers");
                    }
                    Set<CostCenter> ccs = new LinkedHashSet<>();
                    for (Long id : ids) {
                        CostCenter cc = costCenterService.getEntity(id);
                        if (!cc.getBranch().getId().equals(branch.getId())) {
                            throw new BusinessException("Cost center " + cc.getCode() + " belongs to "
                                    + cc.getBranch().getName() + ", not to " + branch.getName());
                        }
                        ccs.add(cc);
                        coveredCostCenters.add(cc.getId());
                    }
                    row.setCostCenters(ccs);
                }
                rows.add(row);
            }
        }

        CostCenter def = request.getDefaultCostCenterId() != null
                ? costCenterService.getUsable(request.getDefaultCostCenterId())
                : null;
        if (def != null && !global && !wholeBranches.contains(def.getBranch().getId()) && !coveredCostCenters.contains(def.getId())) {
            throw new BusinessException("The default cost center " + def.getCode() + " must be one the user has access to");
        }

        for (UserAccessScope old : scopeRepository.findForUser(user.getId())) {
            scopeRepository.delete(old);
        }
        scopeRepository.flush();
        scopeRepository.saveAll(rows);

        UserDimensionDefault current = defaultRepository.findForUser(user.getId()).orElse(null);
        if (def == null) {
            if (current != null) {
                defaultRepository.delete(current);
            }
        } else {
            if (current == null) {
                current = new UserDimensionDefault();
                current.setUser(user);
            }
            current.setCostCenter(def);
            defaultRepository.save(current);
        }
        return get(user.getId());
    }

    /** What the signed-in user may pick: their branches, cost centers and default. */
    @Transactional
    public MyScopeResponse myScope() {
        AccessScope scope = accessScopeService.current();
        List<BranchResponse> branches = branchService.list(true, null);
        List<CostCenterResponse> costCenters = costCenterService.list(null, true, null);
        CostCenter def = dimensionResolver.currentUserDefault();
        boolean defaultUsable = scope.coversCostCenter(def.getId());
        return MyScopeResponse.builder()
                .global(scope.global())
                .branches(branches)
                .costCenters(costCenters)
                .defaultCostCenterId(defaultUsable ? def.getId() : (costCenters.isEmpty() ? null : costCenters.get(0).getId()))
                .defaultBranchId(defaultUsable ? def.getBranch().getId() : (costCenters.isEmpty() ? null : costCenters.get(0).getBranchId()))
                .selfBalancingBranches(Boolean.TRUE.equals(settingsService.settings().getSelfBalancingBranches()))
                .build();
    }

    private User user(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private static String summary(List<UserAccessScope> rows, boolean global) {
        if (global) {
            return "Global (every branch and cost center)";
        }
        if (rows.isEmpty()) {
            return "Head Office only (not set yet)";
        }
        List<String> parts = new ArrayList<>();
        rows.stream().sorted(Comparator.comparing(r -> r.getBranch().getName())).forEach(r -> {
            if (Boolean.TRUE.equals(r.getAllCostCenters())) {
                parts.add(r.getBranch().getName() + " (all cost centers)");
            } else {
                long n = r.getCostCenters().stream().filter(cc -> !Boolean.TRUE.equals(cc.getDeleted())).count();
                parts.add(r.getBranch().getName() + " (" + n + " cost center" + (n == 1 ? "" : "s") + ")");
            }
        });
        return String.join(", ", parts);
    }
}
