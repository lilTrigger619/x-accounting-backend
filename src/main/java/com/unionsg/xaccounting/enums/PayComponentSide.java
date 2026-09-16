package com.unionsg.xaccounting.enums;

/**
 * Which side of the payslip a component affects. This is the distinction the accounting
 * treatment hinges on: an EARNING and an EMPLOYER_CONTRIBUTION both become employer expense,
 * but only an EMPLOYEE_DEDUCTION reduces the employee's own net pay.
 */
public enum PayComponentSide {
    EARNING,
    EMPLOYEE_DEDUCTION,
    EMPLOYER_CONTRIBUTION
}
