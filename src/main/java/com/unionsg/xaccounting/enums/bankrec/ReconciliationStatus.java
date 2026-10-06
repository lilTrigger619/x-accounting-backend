package com.unionsg.xaccounting.enums.bankrec;

/** Lifecycle of a bank reconciliation. Only DRAFT, IN_PROGRESS and REOPENED ones can be worked on. */
public enum ReconciliationStatus {
    DRAFT,
    IN_PROGRESS,
    RECONCILED,
    REOPENED,
    CANCELLED;

    public boolean isOpen() {
        return this == DRAFT || this == IN_PROGRESS || this == REOPENED;
    }
}
