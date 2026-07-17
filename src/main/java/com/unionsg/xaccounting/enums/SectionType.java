package com.unionsg.xaccounting.enums;

public enum SectionType {
    /**
     * Legacy type retained for backward compatibility.
     * Treated as a DETAIL-like leaf by the validation layer.
     */
    SECTION,

    GROUP,
    DETAIL,
    SUBTOTAL,
    TOTAL
}



