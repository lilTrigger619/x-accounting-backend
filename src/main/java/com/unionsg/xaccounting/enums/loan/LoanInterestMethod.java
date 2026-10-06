package com.unionsg.xaccounting.enums.loan;

/**
 * How the schedule's interest and principal are worked out.
 * <ul>
 *   <li>SIMPLE: flat interest on the financed amount every period, equal principal.</li>
 *   <li>FIXED_INSTALLMENT: equal total installments (annuity); interest on the reducing balance.</li>
 *   <li>REDUCING_BALANCE: equal principal; interest on the reducing balance, so installments fall.</li>
 *   <li>CUSTOM_SCHEDULE: the user enters each installment's due date and principal.</li>
 * </ul>
 */
public enum LoanInterestMethod {
    SIMPLE,
    FIXED_INSTALLMENT,
    REDUCING_BALANCE,
    CUSTOM_SCHEDULE
}
