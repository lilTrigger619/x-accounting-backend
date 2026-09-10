package com.unionsg.xaccounting.dto.accounting;

import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class AccountingPeriodResponse {
    private Long id;
    private Long financialYearId;
    private String financialYearName;
    private String name;
    private Integer periodNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private AccountingPeriodStatus status;
    private boolean isActive;

    private LocalDateTime lockedAt;
    private String lockedByName;

    private LocalDateTime unlockedAt;
    private String unlockedByName;
    private String reopenReason;

    private LocalDateTime closedAt;
    private String closedByName;
}
