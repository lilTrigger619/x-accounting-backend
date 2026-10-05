package com.unionsg.xaccounting.service.bankrec.engine;

import com.unionsg.xaccounting.enums.bankrec.AmountSignConvention;
import lombok.Builder;

/**
 * How to read one bank's CSV layout. Column fields hold a header name (case-insensitive) or a
 * 1-based column number. Either an amount column or a debit and/or credit column is required.
 */
@Builder(toBuilder = true)
public record CsvColumnMapping(
        String delimiter,
        boolean hasHeaderRow,
        int skipRows,
        String dateFormat,
        String transactionDateColumn,
        String valueDateColumn,
        String descriptionColumn,
        String referenceColumn,
        String debitColumn,
        String creditColumn,
        String amountColumn,
        String balanceColumn,
        String externalIdColumn,
        AmountSignConvention amountSignConvention
) {
}
