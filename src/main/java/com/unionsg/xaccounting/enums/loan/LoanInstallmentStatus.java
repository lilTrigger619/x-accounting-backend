package com.unionsg.xaccounting.enums.loan;

/**
 * Payment status of one installment. OVERDUE is never stored: it is worked out when an unpaid
 * installment's due date has passed. MISSED is set when a user records the installment as missed.
 */
public enum LoanInstallmentStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    MISSED
}
