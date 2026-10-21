package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/**
 * How a period's depreciation charge is worked out. Each constant is served by a
 * {@code DepreciationMethodCalculator}; adding a method means adding a constant and a calculator.
 * <ul>
 *   <li>STRAIGHT_LINE: the remaining depreciable amount spread evenly over the remaining useful life.</li>
 *   <li>REDUCING_BALANCE: a fixed annual rate applied to the opening carrying amount.</li>
 *   <li>NOT_DEPRECIATED: never charged (land, works of art).</li>
 * </ul>
 */
public enum DepreciationMethod implements LabeledEnum {
    STRAIGHT_LINE("Straight Line"),
    REDUCING_BALANCE("Reducing Balance"),
    NOT_DEPRECIATED("Not Depreciated");

    private final String label;

    DepreciationMethod(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
