package com.unionsg.xaccounting.service.analytics;

/**
 * How the BI module groups ledger accounts. Every KPI is a sum over one or more of these
 * classes, so two cards never use two different ideas of, say, "cash".
 */
public enum AccountClass {
    CASH_BANK("Cash & bank", true, true),
    RECEIVABLE("Accounts receivable", true, true),
    OTHER_CURRENT_ASSET("Other current assets", true, true),
    NON_CURRENT_ASSET("Non-current assets", true, true),
    PAYABLE("Accounts payable", false, true),
    LOAN_LIABILITY("Loans payable", false, true),
    OTHER_CURRENT_LIABILITY("Other current liabilities", false, true),
    NON_CURRENT_LIABILITY("Non-current liabilities", false, true),
    EQUITY("Equity", false, true),
    OPERATING_REVENUE("Operating revenue", false, false),
    OTHER_INCOME("Other income", false, false),
    COST_OF_SALES("Cost of sales", true, false),
    OPERATING_EXPENSE("Operating expenses", true, false),
    OTHER_EXPENSE("Other expenses", true, false);

    private final String label;
    private final boolean debitNatured;
    private final boolean balanceSheet;

    AccountClass(String label, boolean debitNatured, boolean balanceSheet) {
        this.label = label;
        this.debitNatured = debitNatured;
        this.balanceSheet = balanceSheet;
    }

    public String getLabel() {
        return label;
    }

    /** True when a debit increases the natural (positive) amount of this class. */
    public boolean isDebitNatured() {
        return debitNatured;
    }

    /** Balance-sheet classes are read as balances at a date; the rest as activity over a period. */
    public boolean isBalanceSheet() {
        return balanceSheet;
    }
}
