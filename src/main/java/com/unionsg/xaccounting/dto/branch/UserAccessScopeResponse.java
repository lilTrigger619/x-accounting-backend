package com.unionsg.xaccounting.dto.branch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class UserAccessScopeResponse {
    private UUID userId;
    private Boolean global;
    private List<BranchScope> branches;
    private Long defaultCostCenterId;
    private String defaultCostCenterCode;
    private String defaultCostCenterName;
    private String defaultBranchName;
    /** Plain-language summary, e.g. "Global" or "Accra (all), Kumasi (2 cost centers)". */
    private String summary;

    @Getter
    @Setter
    @Builder
    public static class BranchScope {
        private Long branchId;
        private String branchCode;
        private String branchName;
        private Boolean allCostCenters;
        private List<CostCenterResponse> costCenters;
    }
}
