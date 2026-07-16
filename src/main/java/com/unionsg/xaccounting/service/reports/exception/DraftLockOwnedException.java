package com.unionsg.xaccounting.service.reports.exception;

public class DraftLockOwnedException extends RuntimeException {

    private final String lockedBy;

    public DraftLockOwnedException(String message, String lockedBy) {
        super(message);
        this.lockedBy = lockedBy;
    }

    public String getLockedBy() {
        return lockedBy;
    }
}

