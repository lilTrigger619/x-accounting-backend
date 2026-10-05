package com.unionsg.xaccounting.enums.deposit;

import com.unionsg.xaccounting.enums.LabeledEnum;


public enum DepositCounterpartyType implements LabeledEnum {
    CUSTOMER("Customer"),
    SUPPLIER("Supplier"),
    EMPLOYEE("Employee"),
    OTHER("Other");

    private final String label;

    DepositCounterpartyType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
