package com.unionsg.xaccounting.enums;

/**
 * DRAFT -> SUBMITTED -> APPROVED -> POSTED -> REVERSED, with SUBMITTED -> REJECTED -> (edit)
 * DRAFT. Journals that don't need approval (module postings, manual journals under the
 * approval threshold) go straight from DRAFT to POSTED.
 */
public enum JournalStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    POSTED,
    REVERSED,
    CANCELLED
}
