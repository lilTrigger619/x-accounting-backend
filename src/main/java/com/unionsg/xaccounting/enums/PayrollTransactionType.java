package com.unionsg.xaccounting.enums;

/**
 * Classifies every financially-relevant payroll transaction for audit and reporting purposes,
 * independent of the JournalType (GENERAL/PAYROLL) used to post it to the GL.
 */
public enum PayrollTransactionType {
    PAYROLL_POSTING,
    PAYROLL_PAYMENT,
    PAYROLL_REVERSAL,
    PAYROLL_ADJUSTMENT,
    SALARY_ADVANCE,
    ADVANCE_REPAYMENT,
    EMPLOYEE_LOAN,
    LOAN_REPAYMENT,
    STATUTORY_PAYMENT,
    PAYROLL_ACCRUAL,
    PAYROLL_ACCRUAL_REVERSAL,
    BONUS,
    COMMISSION,
    REIMBURSEMENT,
    YEAR_END_PAYROLL_ADJUSTMENT
}
