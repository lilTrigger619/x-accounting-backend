package com.unionsg.xaccounting.enums.bankrec;

import com.unionsg.xaccounting.enums.LabeledEnum;
import com.unionsg.xaccounting.enums.settings.MappingKey;

/**
 * Kinds of bank-only items a reconciliation can bring into the ledger. Each one fixes the cash
 * direction (money into or out of the bank) and the default offset account, which is why this
 * is a system-controlled list rather than a configurable one.
 */
public enum ReconciliationAdjustmentType implements LabeledEnum {
    BANK_CHARGE("Bank charges", false, MappingKey.BANK_CHARGES_EXPENSE),
    BANK_INTEREST("Bank interest", true, MappingKey.BANK_INTEREST_INCOME),
    DIRECT_DEBIT("Direct debit", false, MappingKey.BANK_RECONCILIATION_SUSPENSE),
    DIRECT_CREDIT("Direct credit", true, MappingKey.BANK_RECONCILIATION_SUSPENSE),
    UNKNOWN_DEBIT("Unknown bank debit", false, MappingKey.BANK_RECONCILIATION_SUSPENSE),
    UNKNOWN_CREDIT("Unknown bank credit", true, MappingKey.BANK_RECONCILIATION_SUSPENSE);

    private final String label;
    private final boolean moneyIn;
    private final MappingKey defaultOffsetAccount;

    ReconciliationAdjustmentType(String label, boolean moneyIn, MappingKey defaultOffsetAccount) {
        this.label = label;
        this.moneyIn = moneyIn;
        this.defaultOffsetAccount = defaultOffsetAccount;
    }

    @Override
    public String getLabel() {
        return label;
    }

    /** True when the bank balance goes up (Dr Bank); false when it goes down (Cr Bank). */
    public boolean isMoneyIn() {
        return moneyIn;
    }

    public MappingKey getDefaultOffsetAccount() {
        return defaultOffsetAccount;
    }
}
