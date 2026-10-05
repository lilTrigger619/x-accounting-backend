package com.unionsg.xaccounting.service.bankrec.engine;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the uniqueness key that stops the same statement line from being imported twice.
 *
 * <p>When the bank supplies a transaction ID, that ID alone is the key. Otherwise the key is a
 * fingerprint of date, signed amount, reference, description and running balance, plus how many
 * times that same fingerprint has already appeared earlier in the file. The occurrence number
 * keeps two genuinely identical lines in one file (two equal charges on one day) apart, while
 * re-importing the same file, or an overlapping one, produces the same keys and is skipped.</p>
 */
public final class StatementDedupe {

    private StatementDedupe() {
    }

    /** Keys for each row in file order; invalid rows get null. */
    public static List<String> keys(List<ParsedStatementRow> rows) {
        Map<String, Integer> occurrences = new HashMap<>();
        List<String> keys = new ArrayList<>(rows.size());
        for (ParsedStatementRow row : rows) {
            if (!row.isValid()) {
                keys.add(null);
                continue;
            }
            String base = row.externalId() != null
                    ? "X|" + row.externalId().trim().toUpperCase(Locale.ROOT)
                    : "F|" + row.transactionDate()
                    + "|" + row.amount().toPlainString()
                    + "|" + normalize(row.reference())
                    + "|" + normalize(row.description())
                    + "|" + (row.balance() == null ? "" : row.balance().toPlainString());
            int seen = occurrences.merge(base, 1, Integer::sum);
            String key = row.externalId() != null ? base : base + "#" + seen;
            keys.add(sha256(key));
        }
        return keys;
    }

    static String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
