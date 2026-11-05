package com.unionsg.xaccounting.dto.branch;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BranchAccountingSettingsRequest {
    private Boolean selfBalancingBranches;
    /** Chart of Accounts primary key, as the account picker returns it. */
    private Long interBranchAccountId;
    private Boolean journalApprovalRequired;
    private BigDecimal approvalThreshold;
    /** Control-account postings plus employee, vendor, customer and allocation on manual journal lines. */
    private Boolean subledgerJournalsEnabled;
}
