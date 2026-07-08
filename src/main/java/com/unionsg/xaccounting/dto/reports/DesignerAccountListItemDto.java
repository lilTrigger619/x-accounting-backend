package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DesignerAccountListItemDto(
        Long accountEntityId,
        String accountNumber,
        String accountName,
        AccountType accountType,
        String subType,
        AccountStatus status,
        BigDecimal balance,
        Integer displayOrder
) {
}

