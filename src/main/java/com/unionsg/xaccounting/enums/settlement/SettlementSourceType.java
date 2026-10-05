package com.unionsg.xaccounting.enums.settlement;

import com.unionsg.xaccounting.enums.LabeledEnum;

/**
 * A non-cash source that can settle part of an invoice or bill. Customer and supplier payments
 * are not listed here: they keep their own allocation tables, and whatever an invoice's
 * {@code amountPaid} holds beyond the settlements recorded here is treated as cash payments.
 */
public enum SettlementSourceType implements LabeledEnum {
    DEPOSIT("Deposit"),
    DOWNPAYMENT("Downpayment");

    private final String label;

    SettlementSourceType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
