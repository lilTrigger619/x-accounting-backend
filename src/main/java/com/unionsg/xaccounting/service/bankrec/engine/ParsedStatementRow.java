package com.unionsg.xaccounting.service.bankrec.engine;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One CSV row after parsing. {@code error} is set when the row could not be read, in which case
 * the other fields may be null. {@code amount} is signed: deposits positive, withdrawals negative.
 */
public record ParsedStatementRow(
        int rowNumber,
        LocalDate transactionDate,
        LocalDate valueDate,
        String description,
        String reference,
        BigDecimal debit,
        BigDecimal credit,
        BigDecimal amount,
        BigDecimal balance,
        String externalId,
        String error
) {
    public boolean isValid() {
        return error == null;
    }

    static ParsedStatementRow failed(int rowNumber, String error) {
        return new ParsedStatementRow(rowNumber, null, null, null, null, null, null, null, null, null, error);
    }
}
