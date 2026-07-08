package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class JournalPostedBalanceRepositoryImpl implements JournalPostedBalanceRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, BigDecimal> sumPostedBalancesByAccountId(List<Long> accountIds, LocalDate from, LocalDate to) {

        if (accountIds == null || accountIds.isEmpty()) {
            return Map.of();
        }

        // Balance = SUM(debit - credit) with account normal balance considered in SQL would require join to ChartOfAccount.
        // This repo implementation returns SUM(debit-credit) raw.
        // Then SectionAggregator can adjust per account normal balance if needed.

        // TODO in this project: normal balance adjustment is handled in CalculateBalance in P&L.
        // For now, we implement proper normal balance adjustment with joins:

        String jpql = """
            SELECT jl.account.id, 
                   COALESCE(SUM(
                     CASE WHEN coa.normalBalance = 'DEBIT' THEN jl.debitAmount - jl.creditAmount
                          ELSE jl.creditAmount - jl.debitAmount
                     END
                   ), 0)
            FROM JournalLine jl
              JOIN jl.journalEntry je
              JOIN jl.account acc
              JOIN acc.coaClearTo clearTo
              JOIN clearTo.chartOfAccount coa
            WHERE je.status = :status
              AND je.journalDate BETWEEN :from AND :to
              AND acc.id IN :accountIds
            GROUP BY jl.account.id
        """;

        TypedQuery<Object[]> q = em.createQuery(jpql, Object[].class);
        q.setParameter("status", JournalStatus.POSTED);
        q.setParameter("from", from);
        q.setParameter("to", to);
        q.setParameter("accountIds", accountIds);

        List<Object[]> rows = q.getResultList();
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Object[] r : rows) {
            Long accountId = (Long) r[0];
            BigDecimal value = (BigDecimal) r[1];
            result.put(accountId, value);
        }
        return result;
    }
}

