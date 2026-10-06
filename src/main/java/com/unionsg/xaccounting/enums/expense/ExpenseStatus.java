package com.unionsg.xaccounting.enums.expense;

import com.unionsg.xaccounting.enums.LabeledEnum;

/**
 * Lifecycle of an {@code Expense}: a DRAFT can be edited, posted or deleted; a POSTED expense
 * is read-only and can only be reversed; REVERSED is final.
 */
public enum ExpenseStatus implements LabeledEnum {
    DRAFT("Draft"),
    POSTED("Posted"),
    REVERSED("Reversed");

    private final String label;

    ExpenseStatus(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
