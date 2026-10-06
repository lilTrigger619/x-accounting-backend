package com.unionsg.xaccounting.enums;

public enum JournalType {
    GENERAL,
    SALES,
    PURCHASE,
    PAYROLL,
    ADJUSTMENT,
    OPENING_BALANCE,
    CLOSING,
    REVERSING,
    PREPAYMENT,
    DOWNPAYMENT,
    LOAN,
    BANK_TRANSFER,
    DEPOSIT,
    EXPENSE;

    /**
     * Whether a person may pick this type for a journal entered by hand. Every other type is
     * posted by its own module (opening balances, year-end closing, reversals, prepayments...).
     */
    public boolean isManualEntry() {
        return switch (this) {
            case GENERAL, SALES, PURCHASE, PAYROLL, ADJUSTMENT -> true;
            default -> false;
        };
    }
}
