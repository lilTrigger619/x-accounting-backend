package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** Calculated, then submitted, approved and posted. A posted run can only be reversed. */
public enum DepreciationRunStatus implements LabeledEnum {
    CALCULATED("Calculated"),
    SUBMITTED("Submitted for Approval"),
    APPROVED("Approved"),
    POSTED("Posted"),
    CANCELLED("Cancelled"),
    REVERSED("Reversed");

    private final String label;

    DepreciationRunStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
