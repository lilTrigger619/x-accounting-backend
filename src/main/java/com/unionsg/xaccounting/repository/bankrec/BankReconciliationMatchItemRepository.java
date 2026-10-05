package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankReconciliationMatchItem;
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BankReconciliationMatchItemRepository extends JpaRepository<BankReconciliationMatchItem, Long> {

    /** [journalLineId, matched amount] for the given lines, counting only active matches. */
    @Query("""
            select i.journalLine.id, sum(i.amount) from BankReconciliationMatchItem i
            where i.journalLine.id in :lineIds and i.match.status in :statuses
            group by i.journalLine.id
            """)
    List<Object[]> sumMatchedByJournalLine(@Param("lineIds") Collection<Long> lineIds,
                                           @Param("statuses") Collection<MatchStatus> statuses);

    /** Same as {@link #sumMatchedByJournalLine} but only counting reconciliations ending by {@code asOf}. */
    @Query("""
            select i.journalLine.id, sum(i.amount) from BankReconciliationMatchItem i
            where i.journalLine.id in :lineIds and i.match.status in :statuses
              and i.match.reconciliation.periodEnd <= :asOf
            group by i.journalLine.id
            """)
    List<Object[]> sumMatchedByJournalLineAsOf(@Param("lineIds") Collection<Long> lineIds,
                                               @Param("statuses") Collection<MatchStatus> statuses,
                                               @Param("asOf") java.time.LocalDate asOf);

    @Query("""
            select i.statementTransaction.id, sum(i.amount) from BankReconciliationMatchItem i
            where i.statementTransaction.id in :ids and i.match.status in :statuses
              and i.match.reconciliation.periodEnd <= :asOf
            group by i.statementTransaction.id
            """)
    List<Object[]> sumMatchedByStatementAsOf(@Param("ids") Collection<Long> ids,
                                             @Param("statuses") Collection<MatchStatus> statuses,
                                             @Param("asOf") java.time.LocalDate asOf);

    @Query("""
            select i from BankReconciliationMatchItem i
            where i.match.reconciliation.id = :reconciliationId and i.match.status in :statuses
            """)
    List<BankReconciliationMatchItem> findActiveByReconciliation(@Param("reconciliationId") Long reconciliationId,
                                                                 @Param("statuses") Collection<MatchStatus> statuses);

    @Query("""
            select count(i) from BankReconciliationMatchItem i
            where i.statementTransaction.statementImport.id = :importId and i.match.status in :statuses
            """)
    long countActiveForImport(@Param("importId") Long importId,
                              @Param("statuses") Collection<MatchStatus> statuses);

    @Query("""
            select i from BankReconciliationMatchItem i
            where i.statementTransaction.statementImport.id = :importId
            """)
    List<BankReconciliationMatchItem> findAllForImport(@Param("importId") Long importId);
}
