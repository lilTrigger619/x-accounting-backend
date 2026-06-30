package com.unionsg.xaccounting.dto.reports;

import java.math.BigDecimal;

public record ProfitLossAccountDto(

        Long accountId,

        String accountCode,

        String accountName,

        BigDecimal amount

) {
}