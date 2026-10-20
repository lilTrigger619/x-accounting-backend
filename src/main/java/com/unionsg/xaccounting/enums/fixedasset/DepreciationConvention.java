package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/**
 * How much of the first and last month counts toward depreciation.
 * <ul>
 *   <li>DAILY_PRORATA: the days the asset was in service over the days in the month.</li>
 *   <li>FULL_MONTH: the start month counts in full; the disposal month not at all.</li>
 *   <li>MID_MONTH: half a month in the start month and in the disposal month.</li>
 * </ul>
 */
public enum DepreciationConvention implements LabeledEnum {
    DAILY_PRORATA("Daily Pro-rata"),
    FULL_MONTH("Full Month"),
    MID_MONTH("Mid Month");

    private final String label;

    DepreciationConvention(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
