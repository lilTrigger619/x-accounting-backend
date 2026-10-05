package com.unionsg.xaccounting.service.bankrec.engine;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Automatic matching. Pairs one statement line with one book line when their remaining amounts
 * are exactly equal, same direction, and their dates are within the rule's tolerance, then
 * scores the pair:
 *
 * <ul>
 *   <li>exact amount: 40 (always)</li>
 *   <li>date: up to 20, falling off with each day apart</li>
 *   <li>reference or transaction number found on the other side: 25</li>
 *   <li>same cheque/reference number (4+ digits): 15</li>
 *   <li>shared description words: up to 10</li>
 *   <li>customer/supplier name or reference on the statement: 10</li>
 * </ul>
 *
 * Scores are capped at 100. A pair is auto-confirmed only when it scores at least the rule's
 * auto-confirm threshold AND is unambiguous (no other candidate scored the same for either
 * line); otherwise, at or above the suggest threshold, it is only proposed. Rules run in
 * priority order and each later rule only sees what earlier rules left unmatched.
 */
public final class MatchingEngine {

    private static final Pattern NUMBER = Pattern.compile("\\d{4,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "THE", "AND", "FOR", "FROM", "PAYMENT", "TRANSFER", "TRF", "REF", "BANK", "TXN", "POS", "WITH");

    private MatchingEngine() {
    }

    public static List<MatchProposal> run(List<MatchingCandidate> statement, List<MatchingCandidate> book,
                                          List<MatchingCriteria> rules) {
        List<MatchingCriteria> effective = rules == null || rules.isEmpty() ? List.of(MatchingCriteria.standard()) : rules;
        Set<Long> usedStatement = new HashSet<>();
        Set<Long> usedBook = new HashSet<>();
        List<MatchProposal> proposals = new ArrayList<>();

        for (MatchingCriteria rule : effective) {
            List<Scored> scored = new ArrayList<>();
            for (MatchingCandidate s : statement) {
                if (usedStatement.contains(s.id()) || s.signedAmount() == null || s.signedAmount().signum() == 0) {
                    continue;
                }
                for (MatchingCandidate b : book) {
                    if (usedBook.contains(b.id())) {
                        continue;
                    }
                    Scored pair = score(s, b, rule);
                    if (pair != null && pair.score >= rule.suggestThreshold()) {
                        scored.add(pair);
                    }
                }
            }
            scored.sort(Comparator.comparingInt((Scored p) -> -p.score)
                    .thenComparingLong(p -> p.dayGap)
                    .thenComparing(p -> p.statement.id())
                    .thenComparing(p -> p.book.id()));

            for (Scored pair : scored) {
                if (usedStatement.contains(pair.statement.id()) || usedBook.contains(pair.book.id())) {
                    continue;
                }
                boolean ambiguous = scored.stream().anyMatch(other -> other != pair && other.score == pair.score
                        && !usedStatement.contains(other.statement.id()) && !usedBook.contains(other.book.id())
                        && (other.statement.id().equals(pair.statement.id()) || other.book.id().equals(pair.book.id())));
                List<String> reasons = new ArrayList<>(pair.reasons);
                if (ambiguous) {
                    reasons.add("another transaction scored the same, so this needs confirming");
                }
                boolean auto = !ambiguous && pair.score >= rule.autoConfirmThreshold();
                usedStatement.add(pair.statement.id());
                usedBook.add(pair.book.id());
                proposals.add(new MatchProposal(pair.statement.id(), pair.book.id(), pair.statement.signedAmount().abs(),
                        pair.score, auto, rule.name(), reasons));
            }
        }
        return proposals;
    }

    /** Null when the pair can never match under this rule (amount, direction or date). */
    static Scored score(MatchingCandidate s, MatchingCandidate b, MatchingCriteria rule) {
        if (b.signedAmount() == null || s.signedAmount().compareTo(b.signedAmount()) != 0) {
            return null;
        }
        long gap = Math.abs(ChronoUnit.DAYS.between(s.date(), b.date()));
        if (gap > rule.dateToleranceDays()) {
            return null;
        }
        List<String> reasons = new ArrayList<>();
        int score = 40;
        reasons.add("same amount");

        int datePoints = (int) Math.round(20.0 * (1.0 - (double) gap / (rule.dateToleranceDays() + 1)));
        score += datePoints;
        reasons.add(gap == 0 ? "same date" : gap + " day" + (gap == 1 ? "" : "s") + " apart");

        String statementText = normalize(join(s.reference(), s.description(), s.number()));
        boolean referenceHit = rule.matchReference() && (
                containsToken(statementText, b.reference()) || containsToken(normalize(join(b.reference(), b.description())), s.reference()));
        boolean numberHit = rule.matchTransactionNumber() && containsToken(statementText, b.number());
        if (referenceHit || numberHit) {
            score += 25;
            reasons.add(numberHit ? "transaction number on statement" : "reference matches");
        }

        if (rule.matchChequeNumber()) {
            Set<String> statementNumbers = numbers(join(s.reference(), s.description()));
            Set<String> bookNumbers = numbers(join(b.reference(), b.description(), b.counterpartyReference()));
            statementNumbers.retainAll(bookNumbers);
            if (!statementNumbers.isEmpty()) {
                score += 15;
                reasons.add("cheque/reference number " + statementNumbers.iterator().next());
            }
        }

        if (rule.matchDescription()) {
            double similarity = similarity(s.description(), join(b.description(), b.reference()));
            int points = (int) Math.round(similarity * 10);
            if (points > 0) {
                score += points;
                reasons.add("similar description");
            }
        }

        if (rule.matchCounterparty() && (b.counterparty() != null || b.counterpartyReference() != null)) {
            String text = normalize(join(s.description(), s.reference()));
            if (containsToken(text, b.counterparty()) || containsToken(text, b.counterpartyReference())) {
                score += 10;
                reasons.add("customer/supplier matches");
            }
        }

        return new Scored(s, b, Math.min(100, score), gap, reasons);
    }

    static boolean containsToken(String haystackNormalized, String needle) {
        String n = normalize(needle);
        return n.length() >= 3 && haystackNormalized.contains(n);
    }

    static double similarity(String a, String b) {
        Set<String> left = words(a);
        Set<String> right = words(b);
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> union = new HashSet<>(left);
        union.addAll(right);
        left.retainAll(right);
        return (double) left.size() / union.size();
    }

    private static Set<String> words(String text) {
        Set<String> words = new LinkedHashSet<>();
        if (text == null) {
            return words;
        }
        for (String word : text.toUpperCase(Locale.ROOT).split("[^A-Z0-9]+")) {
            if (word.length() >= 3 && !STOP_WORDS.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    private static Set<String> numbers(String text) {
        Set<String> numbers = new HashSet<>();
        if (text == null) {
            return numbers;
        }
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            String number = matcher.group().replaceFirst("^0+(?=\\d{4})", "");
            // A bare year (2026) shows up in dates and document numbers and proves nothing.
            if (number.length() == 4 && number.compareTo("1900") >= 0 && number.compareTo("2100") <= 0) {
                continue;
            }
            numbers.add(number);
        }
        return numbers;
    }

    static String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part).append(' ');
            }
        }
        return sb.toString();
    }

    record Scored(MatchingCandidate statement, MatchingCandidate book, int score, long dayGap, List<String> reasons) {
    }

    /** Remaining amount helper so callers don't repeat the zero checks. */
    public static boolean isOpen(BigDecimal remaining) {
        return remaining != null && remaining.signum() > 0;
    }
}
