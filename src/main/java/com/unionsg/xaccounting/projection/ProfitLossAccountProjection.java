package com.unionsg.xaccounting.projection;

import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.NormalBalance;

import java.math.BigDecimal;

public interface ProfitLossAccountProjection {

    Long getAccountId();

    String getAccountCode();

    String getAccountName();

    AccountType getAccountType();

    NormalBalance getNormalBalance();

    BigDecimal getTotalDebit();

    BigDecimal getTotalCredit();

}