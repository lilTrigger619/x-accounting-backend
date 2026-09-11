package com.unionsg.xaccounting.dto.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unionsg.xaccounting.enums.FinancialYearStatus;
import lombok.AccessLevel;
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

    // Lombok's own isCurrent() getter is suppressed below and replaced with a hand-written,
    // @JsonProperty-annotated one - otherwise Jackson discovers both the field and the Lombok
    // getter as separate properties and emits both "isCurrent" and a stray "current" key
    // (it strips the leading "is" from an is-prefixed boolean getter by default).
    @Getter(AccessLevel.NONE)
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

    @JsonProperty("isCurrent")
    public boolean isCurrent() {
        return isCurrent;
    }
}
