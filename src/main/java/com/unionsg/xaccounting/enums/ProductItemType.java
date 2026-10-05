package com.unionsg.xaccounting.enums;

public enum ProductItemType implements LabeledEnum {
    INVENTORY("Inventory Item"),
    NON_INVENTORY("Non-Inventory Item"),
    SERVICE("Service"),
    BUNDLE("Bundle");

    private final String label;

    ProductItemType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
