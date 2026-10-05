package com.unionsg.xaccounting.dto.downpayment;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class DownpaymentDashboardResponse {
    private Side customer = new Side();
    private Side supplier = new Side();
    private List<DownpaymentResponse> recent = new ArrayList<>();
    private List<DownpaymentAllocationResponse> recentAllocations = new ArrayList<>();

    @Data
    public static class Side {
        private long openCount;
        private long draftCount;
        private BigDecimal totalReceived = BigDecimal.ZERO;
        private BigDecimal totalApplied = BigDecimal.ZERO;
        private BigDecimal totalRefunded = BigDecimal.ZERO;
        private BigDecimal availableBalance = BigDecimal.ZERO;
        /** Available balance older than 90 days. */
        private BigDecimal agedOver90 = BigDecimal.ZERO;
    }
}
