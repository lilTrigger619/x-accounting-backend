package com.unionsg.xaccounting.service.fixedasset.depreciation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Number formatting for depreciation explanations. */
final class Money {

    private Money() {
    }

    static String format(BigDecimal v) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return f.format(v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP));
    }

    static String months(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
