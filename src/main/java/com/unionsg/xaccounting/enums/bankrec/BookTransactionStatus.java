package com.unionsg.xaccounting.enums.bankrec;

/** How much of a posted bank GL line has been cleared against the bank statement. Derived, never stored. */
public enum BookTransactionStatus {
    UNMATCHED,
    PARTIALLY_MATCHED,
    MATCHED
}
