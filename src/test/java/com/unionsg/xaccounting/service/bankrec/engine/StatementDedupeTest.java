package com.unionsg.xaccounting.service.bankrec.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatementDedupeTest {

    private static ParsedStatementRow row(int n, String date, String amount, String description, String externalId) {
        BigDecimal a = new BigDecimal(amount);
        return new ParsedStatementRow(n, LocalDate.parse(date), null, description, null,
                a.signum() < 0 ? a.abs() : BigDecimal.ZERO, a.signum() > 0 ? a : BigDecimal.ZERO, a, null, externalId, null);
    }

    @Test
    void reimportingTheSameFileGivesTheSameKeys() {
        List<ParsedStatementRow> file = List.of(
                row(2, "2026-03-01", "-5.00", "Charge", null),
                row(3, "2026-03-02", "100.00", "Deposit", null));

        assertThat(StatementDedupe.keys(file)).isEqualTo(StatementDedupe.keys(List.copyOf(file)));
    }

    @Test
    void identicalLinesInOneFileStayDistinct() {
        List<String> keys = StatementDedupe.keys(List.of(
                row(2, "2026-03-01", "-5.00", "SMS charge", null),
                row(3, "2026-03-01", "-5.00", "SMS charge", null)));

        assertThat(keys.get(0)).isNotEqualTo(keys.get(1));
    }

    @Test
    void anOverlappingFileReusesKeysForTheLinesItShares() {
        List<String> march = StatementDedupe.keys(List.of(
                row(2, "2026-03-30", "-5.00", "Charge", null),
                row(3, "2026-03-31", "40.00", "Deposit", null)));
        List<String> overlap = StatementDedupe.keys(List.of(
                row(2, "2026-03-31", "40.00", "Deposit", null),
                row(3, "2026-04-01", "-7.00", "Charge", null)));

        assertThat(overlap.get(0)).isEqualTo(march.get(1));
        assertThat(overlap.get(1)).isNotIn(march);
    }

    @Test
    void theBankTransactionIdAloneDecidesWhenPresent() {
        List<String> first = StatementDedupe.keys(List.of(row(2, "2026-03-01", "-5.00", "Charge", "TX-9")));
        List<String> renamed = StatementDedupe.keys(List.of(row(7, "2026-03-01", "-5.00", "Bank charge (corrected)", "tx-9")));

        assertThat(renamed.get(0)).isEqualTo(first.get(0));
    }

    @Test
    void invalidRowsGetNoKey() {
        ParsedStatementRow bad = ParsedStatementRow.failed(4, "Missing amount");

        assertThat(StatementDedupe.keys(List.of(bad))).containsExactly((String) null);
    }
}
