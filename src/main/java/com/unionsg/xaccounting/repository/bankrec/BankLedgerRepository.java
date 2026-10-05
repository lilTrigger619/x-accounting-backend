package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Read-only queries over the posted journal lines of a bank's GL account - the "book" side of a
 * reconciliation. Reconciliation never writes to these rows; it only links to them.
 */
public interface BankLedgerRepository extends JpaRepository<JournalLine, Long> {

    @Query("""
            select coalesce(sum(jl.debitAmount - jl.creditAmount), 0) from JournalLine jl
            join jl.journalEntry je
            where jl.account.accountId = :accountCode and je.status in :statuses
              and je.journalDate <= :asOf
            """)
    BigDecimal balanceAsOf(@Param("accountCode") String accountCode,
                           @Param("statuses") Collection<JournalStatus> statuses,
                           @Param("asOf") LocalDate asOf);

    /**
     * Posted bank GL lines up to {@code asOf} that were not fully covered, as at that date, by
     * active matches of reconciliations ending on or before it. Restricting by period end is what
     * lets a completed reconciliation's report still show what was outstanding when it closed.
     */
    @Query("""
            select jl from JournalLine jl
            join fetch jl.journalEntry je
            where jl.account.accountId = :accountCode and je.status in :statuses
              and je.journalDate <= :asOf
              and (jl.debitAmount + jl.creditAmount) > coalesce((
                    select sum(i.amount) from BankReconciliationMatchItem i
                    where i.journalLine = jl and i.match.status in :activeMatch
                      and i.match.reconciliation.periodEnd <= :asOf), 0)
            order by je.journalDate, jl.id
            """)
    List<JournalLine> findOpenLines(@Param("accountCode") String accountCode,
                                    @Param("statuses") Collection<JournalStatus> statuses,
                                    @Param("asOf") LocalDate asOf,
                                    @Param("activeMatch") Collection<MatchStatus> activeMatch);

    @Query("""
            select jl from JournalLine jl join fetch jl.journalEntry je
            where jl.id in :ids
            """)
    List<JournalLine> findWithJournal(@Param("ids") Collection<Long> ids);
}
