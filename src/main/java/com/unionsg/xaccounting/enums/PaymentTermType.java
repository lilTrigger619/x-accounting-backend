package com.unionsg.xaccounting.enums;

public enum PaymentTermType implements LabeledEnum {
    DUE_ON_RECEIPT("Due on Receipt"),
    NET15("Net 15"),
    NET30("Net 30"),
    NET45("Net 45"),
    NET60("Net 60");

    private final String label;

    PaymentTermType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
