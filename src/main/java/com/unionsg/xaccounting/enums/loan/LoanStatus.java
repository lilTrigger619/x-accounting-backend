package com.unionsg.xaccounting.enums.loan;

/**
 * Loan lifecycle. DRAFT -> APPROVED -> ACTIVE (disbursed) -> PARTIALLY_PAID -> FULLY_PAID ->
 * CLOSED. DEFAULTED marks a loan the counterparty stopped servicing; it can still take payments
 * and is closed (with a write-off for a lent loan) from there. CANCELLED ends a loan before
 * disbursement; REVERSED undoes a disbursed loan and every posting made against it.
 */
public enum LoanStatus {
    DRAFT,
    APPROVED,
    ACTIVE,
    PARTIALLY_PAID,
    FULLY_PAID,
    DEFAULTED,
    CLOSED,
    CANCELLED,
    REVERSED
}
