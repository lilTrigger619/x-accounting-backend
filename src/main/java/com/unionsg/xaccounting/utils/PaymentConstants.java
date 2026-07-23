package com.unionsg.xaccounting.utils;

public final class PaymentConstants {

    private PaymentConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    public static final String RECEIPT_PREFIX = "RCP";

    public static final String RECEIPT_SEPARATOR = "-";

    public static final String SEQUENCE_CODE = "PAYMENT";

    public static final int MAX_MEMO_LENGTH = 500;

    public static final int MAX_REFERENCE_LENGTH = 100;

    public static final String DEFAULT_CURRENCY = "GHS";

    public static final String DEFAULT_SORT_FIELD = "paymentDate";

    public static final String DEFAULT_SORT_DIRECTION = "DESC";
}
