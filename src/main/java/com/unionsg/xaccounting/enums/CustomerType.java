package com.unionsg.xaccounting.enums;

public enum CustomerType implements LabeledEnum {
    INDV("Individual"),
    BUSN("Business");

    private final String label;

    CustomerType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
