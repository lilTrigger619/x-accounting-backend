package com.unionsg.xaccounting.enums.bankrec;

/**
 * PROPOSED matches came from the engine below the auto-confirm threshold and hold their amounts
 * until a user confirms or rejects them. UNMATCHED and REJECTED rows are kept for the audit trail
 * but no longer clear anything.
 */
public enum MatchStatus {
    PROPOSED,
    CONFIRMED,
    REJECTED,
    UNMATCHED;

    public boolean isActive() {
        return this == PROPOSED || this == CONFIRMED;
    }
}
