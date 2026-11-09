package com.unionsg.xaccounting.dto.branch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class BranchAccountingSettingsResponse {
    private Boolean selfBalancingBranches;
    private Long interBranchAccountId;
    private String interBranchAccountCode;
    private String interBranchAccountName;
    private Boolean journalApprovalRequired;
    private BigDecimal approvalThreshold;
    private Boolean subledgerJournalsEnabled;
    private String journalLineEntryMode;
    private LocalDateTime updatedAt;
}
