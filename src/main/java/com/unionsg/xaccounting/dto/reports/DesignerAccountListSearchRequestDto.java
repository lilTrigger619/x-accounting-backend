package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;

public record DesignerAccountListSearchRequestDto(
        String search,
        AccountType accountType,
        AccountStatus status,
        int page,
        int size
) {}

