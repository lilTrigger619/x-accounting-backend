package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** Why an asset leaves the books. RETIREMENT withdraws it from use with no proceeds. */
public enum AssetDisposalType implements LabeledEnum {
    SALE("Sale"),
    SCRAPPED("Scrapped"),
    DONATED("Donated"),
    LOST_OR_STOLEN("Lost or Stolen"),
    RETIREMENT("Retirement");

    private final String label;

    AssetDisposalType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
