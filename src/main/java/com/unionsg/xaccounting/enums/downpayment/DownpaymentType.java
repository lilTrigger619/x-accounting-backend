package com.unionsg.xaccounting.enums.downpayment;

import com.unionsg.xaccounting.enums.LabeledEnum;

/** Which side of the business a downpayment sits on. */
public enum DownpaymentType implements LabeledEnum {
    /** Received from a customer before delivery: a liability until applied to an invoice. */
    CUSTOMER_DOWNPAYMENT("Customer Downpayment"),
    /** Paid to a supplier before delivery: an asset until applied to a supplier bill. */
    SUPPLIER_DOWNPAYMENT("Supplier Downpayment");

    private final String label;

    DownpaymentType(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
