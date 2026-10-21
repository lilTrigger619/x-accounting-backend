package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** Where an asset is in its life. Only ACTIVE assets are depreciated. */
public enum FixedAssetStatus implements LabeledEnum {
    DRAFT("Draft"),
    ACTIVE("Active"),
    FULLY_DEPRECIATED("Fully Depreciated"),
    DISPOSED("Disposed"),
    RETIRED("Retired");

    private final String label;

    FixedAssetStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
