package com.unionsg.xaccounting.dto.deposit;

import com.unionsg.xaccounting.enums.deposit.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositDashboardResponse {
    /** Totals per currency: {"GHS": {"DEPOSIT_PAID": ..., "DEPOSIT_RECEIVED": ...}}. */
    private Map<String, Map<String, DepositTotalsResponse>> totalsByCurrency = new LinkedHashMap<>();
    private Map<String, Long> countByStatus = new LinkedHashMap<>();
    private List<DepositListItemResponse> overdueReturns = new ArrayList<>();
    private List<DepositListItemResponse> upcomingReturns = new ArrayList<>();
    private List<DepositActivityResponse> recentActivity = new ArrayList<>();
}
