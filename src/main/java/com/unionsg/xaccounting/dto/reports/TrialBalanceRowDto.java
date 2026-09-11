package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.AccountType;

import java.math.BigDecimal;

public record TrialBalanceRowDto(

        Long accountId,
        String accountCode,
        String accountName,
        AccountType accountType,

        /** Natural-column presentation: an account's own balance in its own normal-balance
         * column, or in the opposite column if it happens to carry an abnormal (contra) balance -
         * never both, and never forced into its "expected" side. */
        BigDecimal debit,
        BigDecimal credit

) {
}
