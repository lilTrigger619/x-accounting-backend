package com.unionsg.xaccounting.enums.deposit;

import com.unionsg.xaccounting.enums.LabeledEnum;


public enum DepositStatus implements LabeledEnum {
    DRAFT("Draft"),
    ACTIVE("Active"),
    PARTIALLY_APPLIED("Partially Applied"),
    FULLY_APPLIED("Fully Applied"),
    REFUNDED("Refunded"),
    FORFEITED("Forfeited"),
    CANCELLED("Cancelled"),
    REVERSED("Reversed");

    private final String label;

    DepositStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
