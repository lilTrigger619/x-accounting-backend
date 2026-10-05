package com.unionsg.xaccounting.enums.banking;

/** What happened to a bank transfer, as recorded in its activity history. */
public enum BankTransferAction {
    CREATED,
    UPDATED,
    POSTED,
    REVERSED,
    CANCELLED,
    ATTACHMENT_ADDED,
    ATTACHMENT_REMOVED
}
