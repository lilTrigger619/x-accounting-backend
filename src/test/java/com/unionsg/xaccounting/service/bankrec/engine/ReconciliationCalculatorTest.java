package com.unionsg.xaccounting.service.bankrec.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationCalculatorTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void statementEqualsBookLessOutstandingDepositsPlusOutstandingWithdrawals() {
        // Books: 10,000. A 1,500 deposit and a 700 cheque are in the books but not yet on the statement.
        ReconciliationFigures f = ReconciliationCalculator.calculate(
                d("8000"), d("9200"), d("10000"), d("1500"), d("700"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, d("1200"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(f.expectedStatementBalance()).isEqualByComparingTo("9200");
        assertThat(f.difference()).isEqualByComparingTo("0");
        assertThat(f.withinTolerance()).isTrue();
        assertThat(f.statementCheckDifference()).isEqualByComparingTo("0");
    }

    @Test
    void unrecordedBankItemsShowAsADifferenceUntilAdjusted() {
        // Statement shows a 25 bank charge the books do not have yet.
        ReconciliationFigures before = ReconciliationCalculator.calculate(
                d("0"), d("975"), d("1000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, d("975"), d("-25"), BigDecimal.ZERO);
        assertThat(before.difference()).isEqualByComparingTo("-25");
        assertThat(before.withinTolerance()).isFalse();
        assertThat(before.unmatchedStatementTotal()).isEqualByComparingTo("-25");

        // After the charge is posted as an adjustment the GL balance is 975.
        ReconciliationFigures after = ReconciliationCalculator.calculate(
                d("0"), d("975"), d("975"), BigDecimal.ZERO, BigDecimal.ZERO,
                d("25"), BigDecimal.ZERO, d("-25"), d("975"), BigDecimal.ZERO, BigDecimal.ZERO);
        assertThat(after.difference()).isEqualByComparingTo("0");
        assertThat(after.bookBalanceBeforeAdjustments()).isEqualByComparingTo("1000");
        assertThat(after.bankCharges()).isEqualByComparingTo("25");
        assertThat(after.otherAdjustments()).isEqualByComparingTo("0");
    }

    @Test
    void adjustmentsAreBrokenDownIntoChargesInterestAndOther() {
        ReconciliationFigures f = ReconciliationCalculator.calculate(
                d("0"), d("0"), d("0"), BigDecimal.ZERO, BigDecimal.ZERO,
                d("10"), d("4"), d("-56"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        // net -56 = +4 interest - 10 charges - 50 other
        assertThat(f.otherAdjustments()).isEqualByComparingTo("-50");
    }

    @Test
    void toleranceDecidesWhetherASmallDifferenceIsAccepted() {
        ReconciliationFigures within = ReconciliationCalculator.calculate(
                d("0"), d("100.04"), d("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, d("0.05"));
        ReconciliationFigures outside = ReconciliationCalculator.calculate(
                d("0"), d("100.06"), d("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, d("0.05"));

        assertThat(within.withinTolerance()).isTrue();
        assertThat(outside.withinTolerance()).isFalse();
        assertThat(outside.difference()).isEqualByComparingTo("0.06");
    }

    @Test
    void flagsAStatementWhoseLinesDoNotAddUpToItsBalances() {
        ReconciliationFigures f = ReconciliationCalculator.calculate(
                d("100"), d("250"), d("250"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, d("140"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(f.statementCheckDifference()).isEqualByComparingTo("-10");
    }
}
