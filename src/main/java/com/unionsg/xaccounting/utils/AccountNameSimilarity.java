package com.unionsg.xaccounting.utils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scores how alike two account names are, so near-duplicate accounts can be flagged before
 * one is created. Names are compared on their meaningful words: case, punctuation and filler
 * words ("account", "and", "of" ...) are ignored, and a word with a small typo still counts.
 */
public final class AccountNameSimilarity {

    /** Scores at or above this read as "similar". */
    public static final double THRESHOLD = 0.5;

    private static final Set<String> FILLER = Set.of(
            "a", "an", "the", "and", "of", "for", "to", "in", "on", "at", "by", "with",
            "account", "accounts", "acct", "ac", "a/c");

    private AccountNameSimilarity() {
    }

    /** 0 (nothing alike) to 1 (the same name once normalised). */
    public static double score(String query, String candidate) {
        String q = normalise(query);
        String c = normalise(candidate);
        if (q.isEmpty() || c.isEmpty()) {
            return 0;
        }
        if (q.equals(c)) {
            return 1;
        }
        // A whole-name spelling match only counts when it is close: "Cash in Hand" is not "Cash at Bank".
        double spelling = ratio(q, c);
        double best = spelling >= 0.85 ? spelling : 0;
        // "Rent" inside "Office Rent Expense": one name containing the other, as whole words, is a strong signal.
        if (containsWords(c, q) || containsWords(q, c)) {
            best = Math.max(best, 0.75);
        }
        return Math.max(best, wordOverlap(words(q), words(c)));
    }

    public static boolean isSimilar(String query, String candidate) {
        return score(query, candidate) >= THRESHOLD;
    }

    private static boolean containsWords(String outer, String inner) {
        return (" " + outer + " ").contains(" " + inner + " ");
    }

    static String normalise(String value) {
        if (value == null) {
            return "";
        }
        return String.join(" ", words(value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ")));
    }

    private static List<String> words(String normalised) {
        List<String> all = Arrays.stream(normalised.trim().split("\\s+")).filter(w -> !w.isEmpty()).toList();
        List<String> meaningful = all.stream().filter(w -> !FILLER.contains(w)).toList();
        // A name made only of filler words is compared as written.
        return meaningful.isEmpty() ? all : meaningful;
    }

    /** Matched words over all distinct words, where a near-miss spelling counts as a match. */
    private static double wordOverlap(List<String> a, List<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        int matched = 0;
        for (String w : a) {
            if (b.stream().anyMatch(x -> wordsMatch(w, x))) {
                matched++;
            }
        }
        int union = a.size() + b.size() - matched;
        return union == 0 ? 0 : (double) matched / union;
    }

    private static boolean wordsMatch(String a, String b) {
        if (a.equals(b)) {
            return true;
        }
        // Short words must match exactly, or "tax" would match "fax".
        return Math.min(a.length(), b.length()) >= 4 && ratio(a, b) >= 0.8;
    }

    /** 1 minus the edit distance relative to the longer string. */
    private static double ratio(String a, String b) {
        int longest = Math.max(a.length(), b.length());
        return longest == 0 ? 1 : 1 - (double) levenshtein(a, b) / longest;
    }

    /** Edit distance where swapping two neighbouring letters ("bnak") counts as one edit. */
    private static int levenshtein(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }
}
