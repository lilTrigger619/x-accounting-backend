package com.unionsg.xaccounting.repository.fixedasset;

import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.JournalStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Read-only ledger queries the fixed asset module reconciles against. Like the other reports it
 * counts POSTED and REVERSED journals, so a reversed journal and its reversal cancel out.
 */
public interface FixedAssetLedgerRepository extends Repository<JournalLine, Long> {

    /** Debit-minus-credit balance per account on {@code asOf}. */
    @Query("""
            select l.account.id as accountId, coalesce(sum(l.debitAmount - l.creditAmount), 0) as balance
            from JournalLine l
            where l.account.id in :accountIds
              and l.journalEntry.status in :statuses
              and l.journalEntry.deleted = false
              and l.journalEntry.journalDate <= :asOf
            group by l.account.id
            """)
    List<AccountBalance> balancesAsOf(@Param("accountIds") Collection<Long> accountIds,
                                      @Param("asOf") LocalDate asOf,
                                      @Param("statuses") Collection<JournalStatus> statuses);

    /**
     * Lines on the given accounts in {@code [from, to]} from journals the fixed asset module did not
     * post (manual journals, recurring journals, other modules). These explain reconciliation
     * differences and warn about depreciation being booked twice.
     */
    @Query("""
            select l from JournalLine l join fetch l.journalEntry j join fetch l.account a
            where a.id in :accountIds
              and j.status in :statuses
              and j.deleted = false
              and j.journalDate between :from and :to
              and (j.sourceModule is null or j.sourceModule not in :moduleSources)
            order by j.journalDate asc, j.id asc, l.lineNumber asc
            """)
    List<JournalLine> outsideLines(@Param("accountIds") Collection<Long> accountIds,
                                   @Param("from") LocalDate from,
                                   @Param("to") LocalDate to,
                                   @Param("statuses") Collection<JournalStatus> statuses,
                                   @Param("moduleSources") Collection<String> moduleSources);

    /** Every line of the journals posted in {@code [from, to]} that touch any of the given accounts. */
    @Query("""
            select l from JournalLine l join fetch l.journalEntry j join fetch l.account a
            where j.status in :statuses
              and j.deleted = false
              and j.journalDate between :from and :to
              and j.id in (select l2.journalEntry.id from JournalLine l2 where l2.account.id in :accountIds)
            order by j.journalDate asc, j.id asc, l.lineNumber asc
            """)
    List<JournalLine> linesOfJournalsTouching(@Param("accountIds") Collection<Long> accountIds,
                                              @Param("from") LocalDate from,
                                              @Param("to") LocalDate to,
                                              @Param("statuses") Collection<JournalStatus> statuses);

    /** Every line of module journals posted in {@code [from, to]}. */
    @Query("""
            select l from JournalLine l join fetch l.journalEntry j join fetch l.account a
            where j.status in :statuses
              and j.deleted = false
              and j.journalDate between :from and :to
              and j.sourceModule in :moduleSources
            order by j.journalDate asc, j.id asc, l.lineNumber asc
            """)
    List<JournalLine> moduleLines(@Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("statuses") Collection<JournalStatus> statuses,
                                  @Param("moduleSources") Collection<String> moduleSources);

    /** Journals the module posted in {@code [from, to]}, newest first. */
    @Query("""
            select j from JournalEntry j
            where j.sourceModule in :moduleSources
              and j.deleted = false
              and j.journalDate between :from and :to
            order by j.journalDate desc, j.id desc
            """)
    List<JournalEntry> moduleJournals(@Param("from") LocalDate from,
                                      @Param("to") LocalDate to,
                                      @Param("moduleSources") Collection<String> moduleSources);

    interface AccountBalance {
        Long getAccountId();

        BigDecimal getBalance();
    }
}
