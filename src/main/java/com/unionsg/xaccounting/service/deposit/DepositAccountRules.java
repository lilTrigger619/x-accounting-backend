package com.unionsg.xaccounting.service.deposit;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import com.unionsg.xaccounting.exception.BusinessException;

/**
 * Keeps deposits on the balance sheet: a deposit paid may only be held in an asset account and a
 * deposit received only in a liability account, so neither can be booked as income or expense on
 * receipt/payment. Only a forfeiture reaches the P&amp;L, through an expense (paid) or income
 * (received) account.
 */
public final class DepositAccountRules {

    private DepositAccountRules() {
    }

    public static AccountType typeOf(AccountEntity account) {
        if (account == null || account.getCoaClearTo() == null || account.getCoaClearTo().getChartOfAccount() == null) {
            return null;
        }
        return account.getCoaClearTo().getChartOfAccount().getAccountType();
    }

    public static void assertHoldingAccount(AccountEntity account, DepositDirection direction) {
        if (account == null) {
            return;
        }
        AccountType expected = direction == DepositDirection.DEPOSIT_PAID ? AccountType.ASSET : AccountType.LIABILITY;
        AccountType actual = typeOf(account);
        if (actual != null && actual != expected) {
            throw new BusinessException("\"" + account.getAccountName() + "\" (" + account.getAccountId() + ") is "
                    + article(actual) + " account. A " + direction.getLabel().toLowerCase()
                    + " must be held in " + article(expected) + " account, because it is "
                    + (expected == AccountType.ASSET ? "money owed back to the organization" : "money the organization owes back")
                    + ", not " + (expected == AccountType.ASSET ? "an expense" : "income") + ".");
        }
    }

    public static void assertForfeitureAccount(AccountEntity account, DepositDirection direction) {
        if (account == null) {
            return;
        }
        AccountType expected = direction == DepositDirection.DEPOSIT_PAID ? AccountType.EXPENSE : AccountType.INCOME;
        AccountType actual = typeOf(account);
        if (actual != null && actual != expected) {
            throw new BusinessException("\"" + account.getAccountName() + "\" (" + account.getAccountId() + ") is "
                    + article(actual) + " account. Forfeiting a " + direction.getLabel().toLowerCase()
                    + " must post to " + article(expected) + " account.");
        }
    }

    private static String article(AccountType type) {
        String name = type.name().toLowerCase();
        return ("aeiou".indexOf(name.charAt(0)) >= 0 ? "an " : "a ") + name;
    }
}
