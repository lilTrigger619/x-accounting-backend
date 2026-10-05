package com.unionsg.xaccounting.enums.loan;

import com.unionsg.xaccounting.enums.LabeledEnum;

/**
 * How the loan's upfront fees are settled at disbursement. Either way the fee is recognised as
 * an expense (borrowed) or income (lent) when the loan is disbursed.
 */
public enum LoanFeeTreatment implements LabeledEnum {
    /** Netted off the cash that changes hands at disbursement. */
    EXPENSED_IMMEDIATELY("Deducted at disbursement"),
    /** Added to the loan balance and repaid through the schedule. */
    CAPITALIZED("Added to loan balance");

    private final String label;

    LoanFeeTreatment(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
