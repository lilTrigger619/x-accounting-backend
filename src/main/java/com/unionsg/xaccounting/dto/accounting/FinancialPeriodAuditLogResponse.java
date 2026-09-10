package com.unionsg.xaccounting.dto.accounting;

import com.unionsg.xaccounting.enums.FinancialPeriodAction;
import com.unionsg.xaccounting.enums.FinancialPeriodEntityType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FinancialPeriodAuditLogResponse {
    private Long id;
    private FinancialPeriodEntityType entityType;
    private Long entityId;
    private FinancialPeriodAction action;
    private UUID performedById;
    private String performedByName;
    private LocalDateTime performedAt;
    private String previousStatus;
    private String newStatus;
    private String reason;
}
