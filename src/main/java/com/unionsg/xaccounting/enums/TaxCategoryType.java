package com.unionsg.xaccounting.enums;

public enum TaxCategoryType implements LabeledEnum {
    SALES_TAX("Sales Tax"),
    VAT("VAT"),
    WITHHOLDING_TAX("Withholding Tax");

    private final String label;

    TaxCategoryType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
