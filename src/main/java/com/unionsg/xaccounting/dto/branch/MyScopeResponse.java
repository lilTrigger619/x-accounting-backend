package com.unionsg.xaccounting.dto.branch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** What the current user may pick in branch/cost center pickers and report filters. */
@Getter
@Setter
@Builder
public class MyScopeResponse {
    private Boolean global;
    private List<BranchResponse> branches;
    private List<CostCenterResponse> costCenters;
    private Long defaultCostCenterId;
    private Long defaultBranchId;
    private Boolean selfBalancingBranches;
}
