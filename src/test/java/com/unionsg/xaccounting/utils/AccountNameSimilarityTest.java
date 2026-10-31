package com.unionsg.xaccounting.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNameSimilarityTest {

    @Test
    void sameNameIgnoringCaseAndPunctuationIsIdentical() {
        assertThat(AccountNameSimilarity.score("Cash at Bank", "cash-at-bank")).isEqualTo(1.0);
    }

    @Test
    void fillerWordsAreIgnored() {
        assertThat(AccountNameSimilarity.isSimilar("Rent Account", "Rent")).isTrue();
    }

    @Test
    void wholeWordContainmentIsSimilar() {
        assertThat(AccountNameSimilarity.isSimilar("Rent", "Office Rent Expense")).isTrue();
        assertThat(AccountNameSimilarity.isSimilar("Cash", "Cash on Hand")).isTrue();
    }

    @Test
    void typosStillMatch() {
        assertThat(AccountNameSimilarity.isSimilar("Accounts Recievable", "Accounts Receivable")).isTrue();
        assertThat(AccountNameSimilarity.isSimilar("Cash at Bnak", "Cash at Bank")).isTrue();
    }

    @Test
    void sharingOneCommonWordIsNotEnough() {
        assertThat(AccountNameSimilarity.isSimilar("Bank Charges", "Cash at Bank")).isFalse();
        assertThat(AccountNameSimilarity.isSimilar("Cash at Bank", "Cash in Hand")).isFalse();
    }

    @Test
    void fragmentsInsideWordsDoNotMatch() {
        assertThat(AccountNameSimilarity.isSimilar("ca", "Capital")).isFalse();
        assertThat(AccountNameSimilarity.isSimilar("Tax", "Fax Machine")).isFalse();
    }

    @Test
    void blankNamesScoreZero() {
        assertThat(AccountNameSimilarity.score("", "Cash")).isZero();
        assertThat(AccountNameSimilarity.score("Cash", null)).isZero();
    }
}
