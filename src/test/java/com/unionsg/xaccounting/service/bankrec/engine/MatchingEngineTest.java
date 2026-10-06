package com.unionsg.xaccounting.service.bankrec.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchingEngineTest {

    private static final LocalDate D = LocalDate.of(2026, 3, 10);

    private static MatchingCandidate statement(long id, LocalDate date, String amount, String reference, String description) {
        return new MatchingCandidate(id, date, new BigDecimal(amount), reference, description, null, null, null);
    }

    private static MatchingCandidate book(long id, LocalDate date, String amount, String reference, String description,
                                          String number) {
        return new MatchingCandidate(id, date, new BigDecimal(amount), reference, description, number, null, null);
    }

    @Test
    void exactAmountWithMatchingReferenceIsMatchedAutomatically() {
        List<MatchProposal> result = MatchingEngine.run(
                List.of(statement(1, D, "1200.00", "RCP-0042", "Transfer from Acme Ltd")),
                List.of(book(10, D.minusDays(1), "1200.00", "RCP-0042", "Payment received", "JRN-0099")),
                List.of(MatchingCriteria.standard()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).autoConfirm()).isTrue();
        assertThat(result.get(0).confidence()).isGreaterThanOrEqualTo(85);
        assertThat(result.get(0).bookId()).isEqualTo(10L);
    }

    @Test
    void amountAndDateAloneIsOnlySuggested() {
        List<MatchProposal> result = MatchingEngine.run(
                List.of(statement(1, D, "-300.00", null, "POS purchase")),
                List.of(book(10, D, "-300.00", "SPMT-7", "Supplier payment", "JRN-1")),
                List.of(MatchingCriteria.standard()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).autoConfirm()).isFalse();
        assertThat(result.get(0).confidence()).isBetween(60, 84);
    }

    @Test
    void outsideTheDateToleranceNothingMatches() {
        List<MatchProposal> result = MatchingEngine.run(
                List.of(statement(1, D, "50.00", "REF-1", null)),
                List.of(book(10, D.plusDays(4), "50.00", "REF-1", null, "JRN-1")),
                List.of(MatchingCriteria.standard()));

        assertThat(result).isEmpty();
    }

    @Test
    void differentAmountsOrDirectionsNeverMatch() {
        List<MatchProposal> result = MatchingEngine.run(
                List.of(statement(1, D, "50.00", "REF-1", null), statement(2, D, "-75.00", "REF-2", null)),
                List.of(book(10, D, "50.01", "REF-1", null, "J1"), book(11, D, "75.00", "REF-2", null, "J2")),
                List.of(MatchingCriteria.standard()));

        assertThat(result).isEmpty();
    }

    @Test
    void lowConfidenceBelowTheSuggestThresholdIsLeftAlone() {
        MatchingCriteria strict = new MatchingCriteria("Strict", 3, true, true, true, true, true, 95, 90);

        List<MatchProposal> result = MatchingEngine.run(
                List.of(statement(1, D, "80.00", null, "Deposit")),
                List.of(book(10, D.plusDays(2), "80.00", null, "Cash sale", "J1")),
                List.of(strict));

        assertThat(result).isEmpty();
    }

    @Test
    void tiedCandidatesAreNeverAutoConfirmed() {
        MatchingCriteria lenient = new MatchingCriteria("Lenient", 3, false, false, false, false, false, 60, 50);

        List<MatchProposal> result = MatchingEngine.run(
                List.of(statement(1, D, "-20.00", null, "Fee")),
                List.of(book(10, D, "-20.00", null, null, "J1"), book(11, D, "-20.00", null, null, "J2")),
                List.of(lenient));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).autoConfirm()).isFalse();
        assertThat(result.get(0).reasons()).anyMatch(r -> r.contains("scored the same"));
    }

    @Test
    void transactionNumberAndChequeNumberRaiseTheScore() {
        MatchProposal withCheque = MatchingEngine.run(
                List.of(statement(1, D, "-450.00", "CHQ 004512", "Cheque paid")),
                List.of(book(10, D.minusDays(2), "-450.00", "SPMT-0003", "Cheque 004512 to Volta Supplies", "JRN-2026-0004")),
                List.of(MatchingCriteria.standard())).get(0);

        assertThat(withCheque.reasons()).anyMatch(r -> r.startsWith("cheque/reference number 4512"));
        assertThat(withCheque.autoConfirm()).isFalse();
        assertThat(withCheque.confidence()).isGreaterThanOrEqualTo(60);

        MatchProposal withNumber = MatchingEngine.run(
                List.of(statement(1, D, "-450.00", "JRN-2026-0004", null)),
                List.of(book(10, D, "-450.00", null, null, "JRN-2026-0004")),
                List.of(MatchingCriteria.standard())).get(0);
        assertThat(withNumber.autoConfirm()).isTrue();
    }

    @Test
    void eachLineIsUsedOnlyOnceAndRulesRunInOrder() {
        MatchingCriteria sameDay = new MatchingCriteria("Same day", 0, true, true, true, true, true, 60, 60);
        MatchingCriteria wide = new MatchingCriteria("Wide", 5, true, true, true, true, true, 101, 40);

        List<MatchProposal> result = MatchingEngine.run(
                List.of(statement(1, D, "10.00", null, null), statement(2, D, "10.00", null, null)),
                List.of(book(10, D, "10.00", null, null, "J1"), book(11, D.plusDays(4), "10.00", null, null, "J2")),
                List.of(sameDay, wide));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(MatchProposal::bookId).containsExactlyInAnyOrder(10L, 11L);
        assertThat(result.get(0).ruleName()).isEqualTo("Same day");
        assertThat(result.get(1).ruleName()).isEqualTo("Wide");
        assertThat(result.get(1).autoConfirm()).isFalse();
    }
}
