package com.unionsg.xaccounting.enums.deposit;

import com.unionsg.xaccounting.enums.LabeledEnum;

/**
 * Where a deposit sits on the balance sheet. Derived, never chosen: a deposit paid is always an
 * asset and a deposit received always a liability (never income or expense on receipt), and it
 * is non-current when it is not expected back/settled within 12 months of the deposit date.
 */
public enum DepositClassification implements LabeledEnum {
    CURRENT_ASSET("Current Asset"),
    NON_CURRENT_ASSET("Non-current Asset"),
    CURRENT_LIABILITY("Current Liability"),
    NON_CURRENT_LIABILITY("Non-current Liability");

    private final String label;

    DepositClassification(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
