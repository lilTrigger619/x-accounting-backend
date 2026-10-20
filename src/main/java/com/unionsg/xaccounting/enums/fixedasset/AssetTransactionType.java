package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** A movement in an asset's subledger. Each one carries its signed effect on cost, accumulated depreciation and impairment. */
public enum AssetTransactionType implements LabeledEnum {
    ACQUISITION("Acquisition"),
    DEPRECIATION("Depreciation"),
    IMPAIRMENT("Impairment"),
    DISPOSAL("Disposal"),
    RETIREMENT("Retirement"),
    REVERSAL("Reversal");

    private final String label;

    AssetTransactionType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
