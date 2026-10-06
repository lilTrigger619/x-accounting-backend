package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.DrillTarget;
import com.unionsg.xaccounting.dto.analytics.MetricDto;
import com.unionsg.xaccounting.dto.analytics.Period;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Builds KPI cards with consistent change, direction and plain-language explanation. */
public final class Metrics {

    public static final String MONEY = "MONEY";
    public static final String PERCENT = "PERCENT";
    public static final String NUMBER = "NUMBER";

    /** Whether a rise in the metric is good news, bad news, or neither. */
    public enum Polarity { HIGHER_IS_BETTER, LOWER_IS_BETTER, NEUTRAL }

    private Metrics() {
    }

    public static BigDecimal pct(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() == 0) return null;
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 2, RoundingMode.HALF_UP);
    }

    /** Percentage change from previous to current; null when previous is zero. */
    public static BigDecimal growth(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) return null;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 2, RoundingMode.HALF_UP);
    }

    public static MetricDto metric(String key, String label, String format, BigDecimal current, BigDecimal previous,
                                   Polarity polarity, String definition, DrillTarget drill,
                                   Period comparison, String currency) {
        BigDecimal cur = scale(current);
        BigDecimal prev = comparison == null ? null : scale(previous);
        BigDecimal change = cur == null || prev == null ? null : cur.subtract(prev);
        BigDecimal changePct = PERCENT.equals(format) ? null : growth(cur, prev);
        String direction = change == null ? null : change.signum() > 0 ? "UP" : change.signum() < 0 ? "DOWN" : "FLAT";
        String sentiment = sentiment(direction, polarity);
        String explanation = explain(label, format, cur, prev, change, changePct, comparison, currency);
        return new MetricDto(key, label, format, cur, prev, change, changePct, direction, sentiment,
                explanation, definition, drill);
    }

    static String sentiment(String direction, Polarity polarity) {
        if (direction == null || "FLAT".equals(direction) || polarity == Polarity.NEUTRAL) return "NEUTRAL";
        boolean up = "UP".equals(direction);
        return (up == (polarity == Polarity.HIGHER_IS_BETTER)) ? "POSITIVE" : "NEGATIVE";
    }

    static String explain(String label, String format, BigDecimal cur, BigDecimal prev, BigDecimal change,
                          BigDecimal changePct, Period comparison, String currency) {
        if (cur == null) {
            return PERCENT.equals(format) ? label + " cannot be calculated because there is no revenue in this period." : null;
        }
        if (comparison == null) return null;
        if (prev == null) {
            return "No figure for " + comparison.label() + " to compare with.";
        }
        if (change.signum() == 0) {
            return "Unchanged from " + comparison.label() + ".";
        }
        String verb = change.signum() > 0 ? "Up " : "Down ";
        if (PERCENT.equals(format)) {
            return verb + number(change.abs()) + " percentage points from " + number(prev) + "% in " + comparison.label() + ".";
        }
        String amount = MONEY.equals(format) ? money(change.abs(), currency) : number(change.abs());
        String pctText = changePct == null ? "" : " (" + number(changePct.abs()) + "%)";
        String from = MONEY.equals(format) ? money(prev, currency) : number(prev);
        return verb + amount + pctText + " from " + from + " in " + comparison.label() + ".";
    }

    public static String money(BigDecimal v, String currency) {
        return (currency == null ? "" : currency + " ") + number(v);
    }

    public static String number(BigDecimal v) {
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return df.format(v == null ? BigDecimal.ZERO : v);
    }

    public static BigDecimal scale(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }
}
