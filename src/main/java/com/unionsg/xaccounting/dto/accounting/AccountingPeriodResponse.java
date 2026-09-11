package com.unionsg.xaccounting.dto.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import lombok.AccessLevel;
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

    // Lombok's own isActive() getter is suppressed below and replaced with a hand-written,
    // @JsonProperty-annotated one - otherwise Jackson discovers both the field and the Lombok
    // getter as separate properties and emits both "isActive" and a stray "active" key (it
    // strips the leading "is" from an is-prefixed boolean getter by default).
    @Getter(AccessLevel.NONE)
    private boolean isActive;

    private LocalDateTime lockedAt;
    private String lockedByName;

    private LocalDateTime unlockedAt;
    private String unlockedByName;
    private String reopenReason;

    private LocalDateTime closedAt;
    private String closedByName;

    @JsonProperty("isActive")
    public boolean isActive() {
        return isActive;
    }
}
