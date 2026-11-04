package com.unionsg.xaccounting.service.branch;

import java.util.Collection;
import java.util.Set;

/**
 * Where the current user may apply their roles. {@code global} covers every branch and cost
 * center; otherwise {@code costCenterIds} is the exact set (branch rows with "all cost
 * centers" are expanded into it) and {@code branchIds} the branches those belong to.
 */
public record AccessScope(boolean global, Set<Long> branchIds, Set<Long> costCenterIds) {

    public static AccessScope everything() {
        return new AccessScope(true, Set.of(), Set.of());
    }

    public boolean coversBranch(Long branchId) {
        return global || (branchId != null && branchIds.contains(branchId));
    }

    public boolean coversCostCenter(Long costCenterId) {
        return global || (costCenterId != null && costCenterIds.contains(costCenterId));
    }

    public boolean coversAllCostCenters(Collection<Long> ids) {
        return global || costCenterIds.containsAll(ids);
    }
}
