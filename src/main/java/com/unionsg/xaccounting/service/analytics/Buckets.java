package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.dto.analytics.Granularity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Splits a period into day / week / month / quarter / year buckets, clipped to the period. A clipped
 * bucket is labelled "(partial)" so a short first or last month is not read as a drop.
 */
public final class Buckets {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter WEEK = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    public record Bucket(LocalDate start, LocalDate end, String label) {
        public boolean contains(LocalDate d) {
            return !d.isBefore(start) && !d.isAfter(end);
        }
    }

    private Buckets() {
    }

    public static List<Bucket> of(LocalDate from, LocalDate to, Granularity g) {
        List<Bucket> out = new ArrayList<>();
        LocalDate cursor = startOf(from, g);
        while (!cursor.isAfter(to)) {
            LocalDate next = next(cursor, g);
            LocalDate s = cursor.isBefore(from) ? from : cursor;
            LocalDate e = next.minusDays(1).isAfter(to) ? to : next.minusDays(1);
            boolean partial = g != Granularity.DAY && (!s.equals(cursor) || !e.equals(next.minusDays(1)));
            out.add(new Bucket(s, e, label(cursor, g) + (partial ? " (partial)" : "")));
            cursor = next;
        }
        return out;
    }

    public static long count(LocalDate from, LocalDate to, Granularity g) {
        long n = 0;
        LocalDate cursor = startOf(from, g);
        while (!cursor.isAfter(to) && n <= 100_000) {
            cursor = next(cursor, g);
            n++;
        }
        return n;
    }

    static LocalDate startOf(LocalDate d, Granularity g) {
        return switch (g) {
            case DAY -> d;
            case WEEK -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> d.withDayOfMonth(1);
            case QUARTER -> d.withDayOfMonth(1).withMonth(((d.getMonthValue() - 1) / 3) * 3 + 1);
            case YEAR -> d.withDayOfYear(1);
        };
    }

    static LocalDate next(LocalDate start, Granularity g) {
        return switch (g) {
            case DAY -> start.plusDays(1);
            case WEEK -> start.plusWeeks(1);
            case MONTH -> start.plusMonths(1);
            case QUARTER -> start.plusMonths(3);
            case YEAR -> start.plusYears(1);
        };
    }

    static String label(LocalDate start, Granularity g) {
        return switch (g) {
            case DAY -> start.format(DAY);
            case WEEK -> "Week of " + start.format(WEEK);
            case MONTH -> start.format(MONTH);
            case QUARTER -> "Q" + ((start.getMonthValue() - 1) / 3 + 1) + " " + start.getYear();
            case YEAR -> String.valueOf(start.getYear());
        };
    }
}
