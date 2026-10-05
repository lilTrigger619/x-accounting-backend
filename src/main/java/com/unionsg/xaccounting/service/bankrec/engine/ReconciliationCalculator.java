package com.unionsg.xaccounting.service.bankrec.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Pure arithmetic for {@link ReconciliationFigures}; see that record for the layout. */
public final class ReconciliationCalculator {

    private ReconciliationCalculator() {
    }

    /**
     * @param bookBalance            GL balance of the bank account at period end (includes adjustments)
     * @param outstandingDeposits    unmatched remainder of book receipts up to period end (positive)
     * @param outstandingWithdrawals unmatched remainder of book payments up to period end (positive)
     * @param bankCharges            posted BANK_CHARGE adjustments (positive)
     * @param bankInterest           posted BANK_INTEREST adjustments (positive)
     * @param adjustmentsNet         signed net of every posted adjustment (money in positive)
     * @param statementMovement      signed sum of statement lines dated inside the period
     * @param unmatchedStatementNet  signed unmatched remainder of statement lines up to period end
     */
    public static ReconciliationFigures calculate(
            BigDecimal openingBankBalance,
            BigDecimal closingBankBalance,
            BigDecimal bookBalance,
            BigDecimal outstandingDeposits,
            BigDecimal outstandingWithdrawals,
            BigDecimal bankCharges,
            BigDecimal bankInterest,
            BigDecimal adjustmentsNet,
            BigDecimal statementMovement,
            BigDecimal unmatchedStatementNet,
            BigDecimal tolerance
    ) {
        BigDecimal opening = nz(openingBankBalance);
        BigDecimal closing = nz(closingBankBalance);
        BigDecimal book = nz(bookBalance);
        BigDecimal deposits = nz(outstandingDeposits);
        BigDecimal withdrawals = nz(outstandingWithdrawals);
        BigDecimal charges = nz(bankCharges);
        BigDecimal interest = nz(bankInterest);
        BigDecimal adjustments = nz(adjustmentsNet);
        BigDecimal movement = nz(statementMovement);
        BigDecimal tol = nz(tolerance).abs();

        BigDecimal other = adjustments.subtract(interest).add(charges);
        BigDecimal expected = book.subtract(deposits).add(withdrawals);
        BigDecimal difference = closing.subtract(expected);

        return new ReconciliationFigures(
                s(opening),
                s(closing),
                s(movement),
                s(opening.add(movement).subtract(closing)),
                s(book.subtract(adjustments)),
                s(charges),
                s(interest),
                s(other),
                s(adjustments),
                s(book),
                s(deposits),
                s(withdrawals),
                s(expected),
                s(nz(unmatchedStatementNet)),
                s(difference),
                s(tol),
                difference.abs().compareTo(tol) <= 0
        );
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal s(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
