package com.unionsg.xaccounting.dto.reports;

import java.math.BigDecimal;

public record BalanceSheetAccountDto(

        Long accountId,

        String accountCode,

        String accountName,

        BigDecimal balance

) {
}
