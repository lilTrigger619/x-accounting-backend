package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** Every action written to the fixed asset audit log. */
public enum FixedAssetAuditAction implements LabeledEnum {
    CREATED("Created"),
    UPDATED("Updated"),
    DELETED("Deleted"),
    ACTIVATED("Activated"),
    ESTIMATE_CHANGED("Estimate Changed"),
    IMPAIRED("Impaired"),
    DISPOSED("Disposed"),
    RETIRED("Retired"),
    TRANSACTION_REVERSED("Transaction Reversed"),
    RUN_CALCULATED("Depreciation Calculated"),
    RUN_RECALCULATED("Depreciation Recalculated"),
    RUN_SUBMITTED("Run Submitted"),
    RUN_APPROVED("Run Approved"),
    RUN_REJECTED("Run Sent Back"),
    RUN_POSTED("Depreciation Posted"),
    RUN_CANCELLED("Run Cancelled"),
    RUN_REVERSED("Depreciation Reversed"),
    DEPRECIATED("Depreciated");

    private final String label;

    FixedAssetAuditAction(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
