package com.unionsg.xaccounting.enums.loan;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** Whether the organization received the loan (BORROWED_LOAN) or provided it (LENT_LOAN). */
public enum LoanDirection implements LabeledEnum {
    BORROWED_LOAN("Borrowed Loan"),
    LENT_LOAN("Lent Loan");

    private final String label;

    LoanDirection(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
