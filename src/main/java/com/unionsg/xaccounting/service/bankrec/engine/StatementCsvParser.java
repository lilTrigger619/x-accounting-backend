package com.unionsg.xaccounting.service.bankrec.engine;

import com.unionsg.xaccounting.enums.bankrec.AmountSignConvention;
import com.unionsg.xaccounting.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads bank statement CSV text with a {@link CsvColumnMapping}. Handles quoted fields
 * (including embedded delimiters, quotes and line breaks), thousands separators, currency
 * symbols, bracketed negatives and DR/CR suffixes. Bad rows are returned with an error instead
 * of failing the whole file; a bad mapping fails straight away.
 */
public final class StatementCsvParser {

    /** Tried in order when the mapping has no date format. Day-first comes before month-first. */
    private static final List<String> FALLBACK_DATE_FORMATS = List.of(
            "yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy", "dd-MM-yyyy", "d-M-yyyy", "dd.MM.yyyy",
            "yyyy/MM/dd", "dd MMM yyyy", "d MMM yyyy", "dd-MMM-yyyy", "d-MMM-yyyy", "dd/MM/yy",
            "MM/dd/yyyy", "yyyyMMdd");

    private StatementCsvParser() {
    }

    public static List<ParsedStatementRow> parse(String content, CsvColumnMapping mapping) {
        if (content == null || content.isBlank()) {
            throw new BusinessException("The statement file is empty");
        }
        validateMapping(mapping);
        char delimiter = resolveDelimiter(mapping.delimiter());
        List<List<String>> records = splitRecords(stripBom(content), delimiter);

        int index = Math.max(0, mapping.skipRows());
        List<String> header = null;
        if (mapping.hasHeaderRow()) {
            while (index < records.size() && isBlankRecord(records.get(index))) {
                index++;
            }
            if (index >= records.size()) {
                throw new BusinessException("The statement file has no header row");
            }
            header = records.get(index);
            index++;
        }

        Columns columns = new Columns(header, mapping);
        List<ParsedStatementRow> rows = new ArrayList<>();
        for (int i = index; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (isBlankRecord(record)) {
                continue;
            }
            rows.add(parseRow(i + 1, record, columns, mapping));
        }
        return rows;
    }

    private static void validateMapping(CsvColumnMapping mapping) {
        if (mapping == null) {
            throw new BusinessException("A column mapping is required to import a statement");
        }
        if (isBlank(mapping.transactionDateColumn())) {
            throw new BusinessException("Map the transaction date column");
        }
        if (isBlank(mapping.amountColumn()) && isBlank(mapping.debitColumn()) && isBlank(mapping.creditColumn())) {
            throw new BusinessException("Map either an amount column or debit/credit columns");
        }
        if (!mapping.hasHeaderRow()) {
            for (String column : new String[]{mapping.transactionDateColumn(), mapping.valueDateColumn(),
                    mapping.descriptionColumn(), mapping.referenceColumn(), mapping.debitColumn(),
                    mapping.creditColumn(), mapping.amountColumn(), mapping.balanceColumn(), mapping.externalIdColumn()}) {
                if (!isBlank(column) && parseColumnNumber(column) == null) {
                    throw new BusinessException("Column \"" + column + "\" must be a column number because the file has no header row");
                }
            }
        }
    }

    private static ParsedStatementRow parseRow(int rowNumber, List<String> record, Columns columns, CsvColumnMapping mapping) {
        try {
            LocalDate date = parseDate(columns.get(record, columns.transactionDate), mapping.dateFormat());
            if (date == null) {
                return ParsedStatementRow.failed(rowNumber, "Missing transaction date");
            }
            LocalDate valueDate = parseDate(columns.get(record, columns.valueDate), mapping.dateFormat());

            BigDecimal debit = parseAmount(columns.get(record, columns.debit));
            BigDecimal credit = parseAmount(columns.get(record, columns.credit));
            BigDecimal amount;
            if (debit != null || credit != null) {
                debit = debit == null ? BigDecimal.ZERO : debit.abs();
                credit = credit == null ? BigDecimal.ZERO : credit.abs();
                amount = credit.subtract(debit);
            } else {
                BigDecimal raw = parseAmount(columns.get(record, columns.amount));
                if (raw == null) {
                    return ParsedStatementRow.failed(rowNumber, "Missing amount");
                }
                amount = mapping.amountSignConvention() == AmountSignConvention.POSITIVE_IS_DEBIT ? raw.negate() : raw;
                debit = amount.signum() < 0 ? amount.abs() : BigDecimal.ZERO;
                credit = amount.signum() > 0 ? amount : BigDecimal.ZERO;
            }
            if (amount.signum() == 0) {
                return ParsedStatementRow.failed(rowNumber, "Amount is zero");
            }

            return new ParsedStatementRow(
                    rowNumber,
                    date,
                    valueDate,
                    trimToNull(columns.get(record, columns.description)),
                    trimToNull(columns.get(record, columns.reference)),
                    scale(debit),
                    scale(credit),
                    scale(amount),
                    scale(parseAmount(columns.get(record, columns.balance))),
                    trimToNull(columns.get(record, columns.externalId)),
                    null);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return ParsedStatementRow.failed(rowNumber, e.getMessage());
        }
    }

    static LocalDate parseDate(String value, String format) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        // Spreadsheet exports often carry a time part ("2026-01-31 00:00:00").
        if (text.length() > 10 && text.matches("^\\d{4}-\\d{2}-\\d{2}[ T].*")) {
            text = text.substring(0, 10);
        }
        if (!isBlank(format)) {
            try {
                return LocalDate.parse(text, formatter(format));
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Date \"" + text + "\" does not match the format " + format);
            }
        }
        for (String candidate : FALLBACK_DATE_FORMATS) {
            try {
                return LocalDate.parse(text, formatter(candidate));
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        throw new IllegalArgumentException("Unrecognised date \"" + text + "\"; set a date format on the mapping");
    }

    /** Strict, so 31/02 is an error rather than quietly becoming 28/02. Strict needs "u" for the year. */
    private static DateTimeFormatter formatter(String pattern) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern.replace('y', 'u'))
                .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);
    }

    static BigDecimal parseAmount(String value) {
        String text = trimToNull(value);
        if (text == null || text.equals("-")) {
            return null;
        }
        boolean negative = false;
        String upper = text.toUpperCase(Locale.ROOT);
        if (upper.endsWith("DR")) {
            negative = true;
            text = text.substring(0, text.length() - 2);
        } else if (upper.endsWith("CR")) {
            text = text.substring(0, text.length() - 2);
        }
        text = text.trim();
        if (text.startsWith("(") && text.endsWith(")")) {
            negative = !negative;
            text = text.substring(1, text.length() - 1);
        }
        if (text.endsWith("-")) {
            negative = !negative;
            text = text.substring(0, text.length() - 1);
        }
        String cleaned = text.replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            throw new IllegalArgumentException("Invalid amount \"" + value.trim() + "\"");
        }
        try {
            BigDecimal amount = new BigDecimal(cleaned);
            return negative ? amount.negate() : amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount \"" + value.trim() + "\"");
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    // ---- CSV tokenising -------------------------------------------------------------------

    static List<List<String>> splitRecords(String content, char delimiter) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == delimiter) {
                current.add(field.toString());
                field.setLength(0);
            } else if (c == '\r') {
                // handled with the following \n (or on its own for old Mac line endings)
                if (i + 1 >= content.length() || content.charAt(i + 1) != '\n') {
                    current.add(field.toString());
                    field.setLength(0);
                    records.add(current);
                    current = new ArrayList<>();
                }
            } else if (c == '\n') {
                current.add(field.toString());
                field.setLength(0);
                records.add(current);
                current = new ArrayList<>();
            } else {
                field.append(c);
            }
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            records.add(current);
        }
        return records;
    }

    private static char resolveDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isEmpty()) {
            return ',';
        }
        String d = delimiter.trim().toLowerCase(Locale.ROOT);
        if (d.equals("\\t") || d.equals("tab") || delimiter.equals("\t")) {
            return '\t';
        }
        if (d.isEmpty()) {
            return ',';
        }
        return d.charAt(0);
    }

    private static String stripBom(String content) {
        return content.startsWith("﻿") ? content.substring(1) : content;
    }

    private static boolean isBlankRecord(List<String> record) {
        return record.stream().allMatch(StatementCsvParser::isBlank);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Integer parseColumnNumber(String column) {
        String text = column.trim();
        if (!text.matches("\\d+")) {
            return null;
        }
        int number = Integer.parseInt(text);
        return number >= 1 ? number : null;
    }

    /** Resolves each mapped column to a 0-based index once, from the header or a column number. */
    private static final class Columns {
        final int transactionDate;
        final int valueDate;
        final int description;
        final int reference;
        final int debit;
        final int credit;
        final int amount;
        final int balance;
        final int externalId;

        Columns(List<String> header, CsvColumnMapping mapping) {
            transactionDate = resolve(header, mapping.transactionDateColumn(), true);
            valueDate = resolve(header, mapping.valueDateColumn(), false);
            description = resolve(header, mapping.descriptionColumn(), false);
            reference = resolve(header, mapping.referenceColumn(), false);
            debit = resolve(header, mapping.debitColumn(), false);
            credit = resolve(header, mapping.creditColumn(), false);
            amount = resolve(header, mapping.amountColumn(), false);
            balance = resolve(header, mapping.balanceColumn(), false);
            externalId = resolve(header, mapping.externalIdColumn(), false);
        }

        private static int resolve(List<String> header, String column, boolean required) {
            if (isBlank(column)) {
                return -1;
            }
            if (header != null) {
                for (int i = 0; i < header.size(); i++) {
                    if (header.get(i) != null && header.get(i).trim().equalsIgnoreCase(column.trim())) {
                        return i;
                    }
                }
            }
            Integer number = parseColumnNumber(column);
            if (number != null) {
                return number - 1;
            }
            throw new BusinessException("Column \"" + column + "\" was not found in the file's header row"
                    + (required ? "" : " (clear it from the mapping if the file does not have it)"));
        }

        String get(List<String> record, int index) {
            return index >= 0 && index < record.size() ? record.get(index) : null;
        }
    }
}
