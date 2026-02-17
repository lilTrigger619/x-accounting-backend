package com.unionsg.xaccounting.enums;

public enum PaymentTermType {
    DUE_ON_RECEIPT("due_on_receipt"),
    NET15("net15"),
    NET30("net30"),
    NET45("net45"),
    NET60("net60");

    private PaymentTermType(String lable){ }
}
