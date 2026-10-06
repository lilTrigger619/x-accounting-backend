package com.unionsg.xaccounting.repository.bankrec;

import com.unionsg.xaccounting.entity.bankrec.BankStatementTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BankStatementTransactionRepository extends JpaRepository<BankStatementTransaction, Long> {

    @Query("""
            select t.dedupeKey from BankStatementTransaction t
            where t.bankAccount.id = :bankAccountId and t.dedupeKey in :keys
            """)
    List<String> findExistingDedupeKeys(@Param("bankAccountId") Long bankAccountId,
                                        @Param("keys") Collection<String> keys);

    /**
     * Statement lines up to {@code asOf} that still had an amount left to match as at that date,
     * counting only active matches of reconciliations ending on or before it.
     */
    @Query("""
            select t from BankStatementTransaction t
            where t.bankAccount.id = :bankAccountId and t.transactionDate <= :asOf
              and abs(t.amount) > coalesce((
                    select sum(i.amount) from BankReconciliationMatchItem i
                    where i.statementTransaction = t and i.match.status in :activeMatch
                      and i.match.reconciliation.periodEnd <= :asOf), 0)
            order by t.transactionDate, t.id
            """)
    List<BankStatementTransaction> findOpenAsOf(@Param("bankAccountId") Long bankAccountId,
                                                @Param("asOf") LocalDate asOf,
                                                @Param("activeMatch") Collection<com.unionsg.xaccounting.enums.bankrec.MatchStatus> activeMatch);

    @Query("""
            select coalesce(sum(t.amount), 0) from BankStatementTransaction t
            where t.bankAccount.id = :bankAccountId and t.transactionDate between :from and :to
            """)
    java.math.BigDecimal sumMovement(@Param("bankAccountId") Long bankAccountId,
                                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    List<BankStatementTransaction> findByStatementImportId(Long statementImportId);

    @Query("""
            select t from BankStatementTransaction t
            where t.bankAccount.id = :bankAccountId
              and t.transactionDate between :from and :to
            order by t.transactionDate desc, t.id desc
            """)
    List<BankStatementTransaction> findInRange(@Param("bankAccountId") Long bankAccountId,
                                               @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select count(t) from BankStatementTransaction t
            where t.bankAccount.id = :bankAccountId and t.matchedAmount < abs(t.amount)
            """)
    long countOpen(@Param("bankAccountId") Long bankAccountId);
}
