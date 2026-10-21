package com.unionsg.xaccounting.enums.fixedasset;

import com.unionsg.xaccounting.enums.LabeledEnum;

/**
 * What the acquisition journal credits when an asset is activated.
 * <ul>
 *   <li>BANK_PAYMENT: Dr cost, Cr the chosen bank account.</li>
 *   <li>OFFSET_ACCOUNT: Dr cost, Cr a chosen GL account (other creditors, opening balance equity).</li>
 *   <li>ALREADY_IN_LEDGER: no journal; the cost was already posted (for example by an expense).</li>
 * </ul>
 */
public enum AssetAcquisitionSource implements LabeledEnum {
    BANK_PAYMENT("Paid from Bank"),
    OFFSET_ACCOUNT("Against Another Account"),
    ALREADY_IN_LEDGER("Already in the Ledger");

    private final String label;

    AssetAcquisitionSource(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
