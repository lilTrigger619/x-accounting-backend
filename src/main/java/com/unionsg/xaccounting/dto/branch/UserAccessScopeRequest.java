package com.unionsg.xaccounting.dto.branch;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Replaces a user's whole scope: Global, or one entry per branch. */
@Getter
@Setter
public class UserAccessScopeRequest {

    private Boolean global;

    private List<BranchScope> branches = new ArrayList<>();

    /** Where the user's postings land when a document names no cost center. */
    private Long defaultCostCenterId;

    @Getter
    @Setter
    public static class BranchScope {
        private Long branchId;
        /** True covers every cost center of the branch, including ones created later. */
        private Boolean allCostCenters;
        private List<Long> costCenterIds = new ArrayList<>();
    }
}
