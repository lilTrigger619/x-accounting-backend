package com.unionsg.xaccounting.dto.accounting;

import com.unionsg.xaccounting.enums.FinancialYearStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FinancialYearResponse {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private FinancialYearStatus status;
    private boolean isCurrent;
    private boolean hasOpeningBalance;

    private int periodCount;
    private int openPeriodCount;
    private int lockedPeriodCount;
    private int closedPeriodCount;

    private LocalDateTime closedAt;
    private UUID closedById;
    private String closedByName;
    private Long closingJournalId;

    private LocalDateTime reopenedAt;
    private String reopenedByName;
    private String reopenReason;

    private BigDecimal totalRevenue;
    private BigDecimal totalExpense;
    private BigDecimal netProfitLoss;
    private BigDecimal retainedEarningsMovement;
}
