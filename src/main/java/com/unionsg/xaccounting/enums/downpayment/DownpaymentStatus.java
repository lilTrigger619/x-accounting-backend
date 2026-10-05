package com.unionsg.xaccounting.enums.downpayment;

import com.unionsg.xaccounting.enums.LabeledEnum;

public enum DownpaymentStatus implements LabeledEnum {
    /** Saved but not posted; can still be edited or deleted. */
    DRAFT("Draft"),
    /** Posted, nothing applied or refunded yet. */
    OPEN("Open"),
    /** Posted, part of it applied and/or refunded, some still available. */
    PARTIALLY_APPLIED("Partially Applied"),
    /** Fully used against invoices/bills. */
    FULLY_APPLIED("Fully Applied"),
    /** Fully returned to the customer / by the supplier. */
    REFUNDED("Refunded"),
    /** Fully used by a mix of applications and refunds. */
    CLOSED("Closed"),
    /** A draft that was abandoned without posting. */
    CANCELLED("Cancelled"),
    /** A posted downpayment whose journal was reversed. */
    REVERSED("Reversed");

    private final String label;

    DownpaymentStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
