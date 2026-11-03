package com.unionsg.xaccounting.service.branch;

import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.branch.CostCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns a report's filter (nothing = Global, a set of branches, a set of cost centers) into
 * the exact cost centers to sum, always narrowed to what the user may see. Returns null when
 * the report should cover everything (a Global user asking for Global), so the unfiltered,
 * consolidated query runs.
 */
@Component
@RequiredArgsConstructor
public class ReportScopeResolver {

    private final AccessScopeService accessScopeService;
    private final CostCenterRepository costCenterRepository;

    @Transactional(readOnly = true)
    public Set<Long> costCenterFilter(List<Long> branchIds, List<Long> costCenterIds) {
        AccessScope scope = accessScopeService.current();
        boolean noBranches = branchIds == null || branchIds.isEmpty();
        boolean noCostCenters = costCenterIds == null || costCenterIds.isEmpty();

        if (noBranches && noCostCenters) {
            if (scope.global()) {
                return null;
            }
            return nonEmpty(scope.costCenterIds());
        }

        Set<Long> wanted = new HashSet<>();
        if (!noBranches) {
            wanted.addAll(costCenterRepository.findByBranchIdInAndDeletedFalse(branchIds)
                    .stream().map(CostCenter::getId).collect(Collectors.toSet()));
        }
        if (!noCostCenters) {
            if (noBranches) {
                wanted.addAll(costCenterIds);
            } else {
                // Both given: the cost centers, limited to the chosen branches.
                wanted.retainAll(Set.copyOf(costCenterIds));
            }
        }
        if (!scope.global()) {
            wanted.retainAll(scope.costCenterIds());
        }
        if (wanted.isEmpty()) {
            throw new BusinessException("None of the chosen branches or cost centers are in your access");
        }
        return wanted;
    }

    private static Set<Long> nonEmpty(Set<Long> ids) {
        return ids.isEmpty() ? Set.of(-1L) : ids;
    }
}
