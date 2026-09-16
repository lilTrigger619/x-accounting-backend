package com.unionsg.xaccounting.enums;

/**
 * The controlled lifecycle of a Payroll Run. Calculation, approval, accounting and payment are
 * deliberately distinct stages: reaching CALCULATED never implies POSTED, and reaching POSTED
 * never implies PAID. See PayrollRun / PayrollRunService for the transition rules.
 */
public enum PayrollRunStatus {
    DRAFT,
    CALCULATING,
    CALCULATED,
    UNDER_REVIEW,
    APPROVED,
    POSTED,
    PAID,
    REVERSED,
    CANCELLED
}
