package com.unionsg.xaccounting.utils;

import com.unionsg.xaccounting.enums.NormalBalance;

import java.math.BigDecimal;

public class CalculateBalance {

    public static BigDecimal calculateBalance(
            BigDecimal debit,
            BigDecimal credit,
            NormalBalance normalBalance
    ) {

        return normalBalance == NormalBalance.DEBIT
                ? debit.subtract(credit)
                : credit.subtract(debit);

    }
}
