package com.unionsg.xaccounting.dto.accounting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class YearEndClosingPreviewResponse {

    private Long financialYearId;
    private String financialYearName;
    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalRevenue;
    private BigDecimal totalExpense;
    private BigDecimal netProfitLoss;

    private int revenueAccountCount;
    private int expenseAccountCount;

    private int totalPeriods;
    private int openPeriods;
    private int lockedPeriods;
    private int closedPeriods;

    private boolean hasOpeningBalance;
    private boolean alreadyClosed;

    private List<String> warnings;
}
