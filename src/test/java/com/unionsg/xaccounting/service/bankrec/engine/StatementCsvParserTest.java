package com.unionsg.xaccounting.service.bankrec.engine;

import com.unionsg.xaccounting.enums.bankrec.AmountSignConvention;
import com.unionsg.xaccounting.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatementCsvParserTest {

    private static CsvColumnMapping debitCredit() {
        return CsvColumnMapping.builder()
                .delimiter(",").hasHeaderRow(true).dateFormat("dd/MM/yyyy")
                .transactionDateColumn("Date").descriptionColumn("Description").referenceColumn("Reference")
                .debitColumn("Debit").creditColumn("Credit").balanceColumn("Balance").externalIdColumn("Txn ID")
                .build();
    }

    @Test
    void readsDebitAndCreditColumnsIntoASignedAmount() {
        String csv = """
                Date,Description,Reference,Debit,Credit,Balance,Txn ID
                01/03/2026,Opening deposit,DEP-1,,"1,500.00","1,500.00",A1
                02/03/2026,"Cheque 004512, rent",004512,250.50,,1249.50,A2
                """;

        List<ParsedStatementRow> rows = StatementCsvParser.parse(csv, debitCredit());

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).amount()).isEqualByComparingTo("1500.00");
        assertThat(rows.get(0).credit()).isEqualByComparingTo("1500.00");
        assertThat(rows.get(0).transactionDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(rows.get(1).amount()).isEqualByComparingTo("-250.50");
        assertThat(rows.get(1).description()).isEqualTo("Cheque 004512, rent");
        assertThat(rows.get(1).externalId()).isEqualTo("A2");
        assertThat(rows.get(1).balance()).isEqualByComparingTo("1249.50");
    }

    @Test
    void readsASingleAmountColumnWithEitherSignConvention() {
        String csv = "Posted,Narrative,Amount\n2026-03-05,Fee,(12.00)\n2026-03-06,Interest,3.10\n2026-03-07,Card,45.00 DR\n";
        CsvColumnMapping mapping = CsvColumnMapping.builder().hasHeaderRow(true)
                .transactionDateColumn("posted").descriptionColumn("NARRATIVE").amountColumn("Amount").build();

        List<ParsedStatementRow> rows = StatementCsvParser.parse(csv, mapping);
        assertThat(rows).extracting(ParsedStatementRow::amount)
                .containsExactly(new BigDecimal("-12.00"), new BigDecimal("3.10"), new BigDecimal("-45.00"));

        List<ParsedStatementRow> inverted = StatementCsvParser.parse(csv,
                mapping.toBuilder().amountSignConvention(AmountSignConvention.POSITIVE_IS_DEBIT).build());
        assertThat(inverted.get(1).amount()).isEqualByComparingTo("-3.10");
    }

    @Test
    void supportsColumnNumbersWhenThereIsNoHeader() {
        String csv = "05/03/2026;ATM withdrawal;-100\n";
        CsvColumnMapping mapping = CsvColumnMapping.builder().delimiter(";").hasHeaderRow(false)
                .transactionDateColumn("1").descriptionColumn("2").amountColumn("3").build();

        ParsedStatementRow row = StatementCsvParser.parse(csv, mapping).get(0);

        assertThat(row.isValid()).isTrue();
        assertThat(row.amount()).isEqualByComparingTo("-100.00");
        assertThat(row.transactionDate()).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void reportsBadRowsWithoutFailingTheFile() {
        String csv = """
                Date,Description,Reference,Debit,Credit,Balance,Txn ID
                31/02/2026,Bad date,,10,,,
                01/03/2026,No amount,,,,,
                01/03/2026,Good,,5,,,
                """;

        List<ParsedStatementRow> rows = StatementCsvParser.parse(csv, debitCredit());

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).isValid()).isFalse();
        assertThat(rows.get(1).error()).isEqualTo("Missing amount");
        assertThat(rows.get(2).isValid()).isTrue();
    }

    @Test
    void failsFastWhenAMappedColumnIsMissing() {
        String csv = "Date,Amount\n01/03/2026,5\n";
        CsvColumnMapping mapping = CsvColumnMapping.builder().hasHeaderRow(true)
                .transactionDateColumn("Date").amountColumn("Value").build();

        assertThatThrownBy(() -> StatementCsvParser.parse(csv, mapping))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("\"Value\" was not found");
    }

    @Test
    void requiresAnAmountMapping() {
        CsvColumnMapping mapping = CsvColumnMapping.builder().hasHeaderRow(true).transactionDateColumn("Date").build();

        assertThatThrownBy(() -> StatementCsvParser.parse("Date\n01/01/2026\n", mapping))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("amount column");
    }
}
