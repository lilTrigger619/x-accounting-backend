package com.unionsg.xaccounting.enums.deposit;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** Paid out by the organization (an asset it expects back) or received from a customer/third party (a liability it owes back). */
public enum DepositDirection implements LabeledEnum {
    DEPOSIT_PAID("Deposit Paid"),
    DEPOSIT_RECEIVED("Deposit Received");

    private final String label;

    DepositDirection(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
