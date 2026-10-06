package com.unionsg.xaccounting.enums.settings;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** The section an accounting mapping is listed under on the Accounting Mappings screen. */
public enum MappingGroup implements LabeledEnum {
    SALES("Sales & Invoicing"),
    PURCHASES("Purchases & Bills"),
    PAYROLL("Payroll"),
    CLOSING("Year-End Closing"),
    PREPAYMENTS("Prepayments"),
    LOANS("Loans"),
    BANKING("Banking"),
    DEPOSITS("Deposits");

    private final String label;

    MappingGroup(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
