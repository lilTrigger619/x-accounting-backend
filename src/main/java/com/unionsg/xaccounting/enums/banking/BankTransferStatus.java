package com.unionsg.xaccounting.enums.banking;

/**
 * Lifecycle of a {@code BankTransfer}: a DRAFT can be edited, posted or cancelled; a POSTED
 * transfer is read-only and can only be reversed; REVERSED and CANCELLED are final.
 */
public enum BankTransferStatus {
    DRAFT,
    POSTED,
    REVERSED,
    CANCELLED
}
