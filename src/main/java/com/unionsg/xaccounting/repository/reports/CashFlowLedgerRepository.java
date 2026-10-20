package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.JournalType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Journal line activity for the cash flow statement, summed per journal and account so each
 * journal's cash legs can be paired with the accounts on its other side.
 */
public interface CashFlowLedgerRepository extends Repository<JournalLine, Long> {

    @Query("""
        SELECT je.id AS journalId, je.journalType AS journalType,
               COALESCE(je.sourceModule, orig.sourceModule) AS sourceModule,
               acc.id AS accountId,
               COALESCE(SUM(jl.debitAmount), 0) AS debit, COALESCE(SUM(jl.creditAmount), 0) AS credit
        FROM JournalLine jl
            JOIN jl.journalEntry je
            JOIN jl.account acc
            LEFT JOIN JournalEntry orig ON orig.id = je.reversalOfJournalId
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.deleted = false
          AND je.journalDate >= :from AND je.journalDate <= :to
        GROUP BY je.id, je.journalType, COALESCE(je.sourceModule, orig.sourceModule), acc.id
        """)
    List<JournalAccountRow> findJournalAccountActivity(@Param("from") LocalDate from, @Param("to") LocalDate to);

    interface JournalAccountRow {
        Long getJournalId();

        JournalType getJournalType();

        String getSourceModule();

        Long getAccountId();

        BigDecimal getDebit();

        BigDecimal getCredit();
    }
}
