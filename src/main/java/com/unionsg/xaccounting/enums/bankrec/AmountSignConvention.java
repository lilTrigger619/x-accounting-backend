package com.unionsg.xaccounting.enums.bankrec;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** How a single signed "Amount" column in a bank CSV should be read. */
public enum AmountSignConvention implements LabeledEnum {
    POSITIVE_IS_CREDIT("Positive amounts are deposits"),
    POSITIVE_IS_DEBIT("Positive amounts are withdrawals");

    private final String label;

    AmountSignConvention(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
