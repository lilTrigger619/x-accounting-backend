package com.unionsg.xaccounting.service.bankrec.engine;

import java.math.BigDecimal;

/**
 * The reconciliation statement:
 *
 * <pre>
 *   Book balance before adjustments
 * + adjustments posted in this reconciliation (interest/credits in, charges/debits out)
 * = book balance (GL at period end)
 * - outstanding deposits      (in the books, not yet on the statement)
 * + outstanding withdrawals   (in the books, not yet on the statement)
 * = expected statement balance
 *   difference = statement closing balance - expected statement balance
 * </pre>
 *
 * Statement lines with nothing matching in the books show up in the difference until they are
 * matched or brought in with an adjustment.
 */
public record ReconciliationFigures(
        BigDecimal openingBankBalance,
        BigDecimal closingBankBalance,
        BigDecimal statementMovement,
        BigDecimal statementCheckDifference,
        BigDecimal bookBalanceBeforeAdjustments,
        BigDecimal bankCharges,
        BigDecimal bankInterest,
        BigDecimal otherAdjustments,
        BigDecimal adjustmentsTotal,
        BigDecimal bookBalance,
        BigDecimal outstandingDeposits,
        BigDecimal outstandingWithdrawals,
        BigDecimal expectedStatementBalance,
        BigDecimal unmatchedStatementTotal,
        BigDecimal difference,
        BigDecimal tolerance,
        boolean withinTolerance
) {
}
