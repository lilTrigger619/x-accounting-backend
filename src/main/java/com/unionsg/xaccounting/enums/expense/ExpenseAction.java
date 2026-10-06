package com.unionsg.xaccounting.enums.expense;

/** What happened to an expense, as recorded in its activity history. */
public enum ExpenseAction {
    CREATED,
    UPDATED,
    POSTED,
    REVERSED,
    ATTACHMENT_ADDED,
    ATTACHMENT_REMOVED
}
