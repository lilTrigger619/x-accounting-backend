package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** One asset in a depreciation run. Only CALCULATED lines are posted. */
public enum DepreciationLineStatus implements LabeledEnum {
    CALCULATED("Calculated"),
    SKIPPED("Skipped"),
    ERROR("Error"),
    POSTED("Posted"),
    REVERSED("Reversed");

    private final String label;

    DepreciationLineStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
