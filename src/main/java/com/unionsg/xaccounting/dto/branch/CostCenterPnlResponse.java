package com.unionsg.xaccounting.dto.branch;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Income and expense by account (rows) and cost center (columns) for a period. */
@Getter
@Setter
@Builder
public class CostCenterPnlResponse {
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<CostCenterResponse> costCenters;
    private List<Row> rows;
    /** Net profit per cost center id. */
    private Map<Long, BigDecimal> netByCostCenter;
    private BigDecimal netTotal;

    @Getter
    @Setter
    @Builder
    public static class Row {
        private Long accountId;
        private String accountCode;
        private String accountName;
        private String accountType;
        /** Signed so income and expense both read positive. */
        private Map<Long, BigDecimal> amounts;
        private BigDecimal total;
    }
}
