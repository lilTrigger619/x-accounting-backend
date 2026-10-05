package com.unionsg.xaccounting.enums.deposit;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** Every way part of a deposit's balance can leave it. */
public enum DepositAllocationType implements LabeledEnum {
    APPLICATION("Applied to Invoice/Bill"),
    REFUND("Refund"),
    FORFEITURE("Forfeiture"),
    TRANSFER("Transfer");

    private final String label;

    DepositAllocationType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
