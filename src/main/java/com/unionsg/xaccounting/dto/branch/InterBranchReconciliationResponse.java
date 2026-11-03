package com.unionsg.xaccounting.dto.branch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class InterBranchReconciliationResponse {
    private LocalDate asOfDate;
    private String interBranchAccountCode;
    private String interBranchAccountName;
    private List<Pair> pairs;
    /** Net of every inter-branch line company-wide; zero when the books are consistent. */
    private BigDecimal consolidatedNet;
    private boolean reconciled;

    /**
     * Branch A's balance against B (positive = B owes A) next to B's balance against A
     * (negative = B owes A). They must add up to zero.
     */
    @Getter
    @Setter
    @Builder
    public static class Pair {
        private Long branchId;
        private String branchCode;
        private String branchName;
        private Long counterpartyBranchId;
        private String counterpartyBranchCode;
        private String counterpartyBranchName;
        private BigDecimal branchBalance;
        private BigDecimal counterpartyBalance;
        private BigDecimal difference;
        private boolean reconciled;
    }
}
