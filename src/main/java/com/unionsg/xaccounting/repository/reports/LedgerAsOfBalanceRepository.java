package com.unionsg.xaccounting.repository.reports;

import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.projection.ProfitLossAccountProjection;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Cumulative, as-of-a-date account balances - the semantics a Balance Sheet and a Trial Balance
 * both need (every posted transaction from account inception through {@code asOfDate}), as
 * distinct from {@link com.unionsg.xaccounting.repository.reports.ProfitAndLossRepository}'s
 * period-activity sums (which are only ever correct for INCOME/EXPENSE accounts over a bounded
 * date range - reusing that query for a balance-sheet account would silently drop everything
 * posted before the range's start date).
 */
public interface LedgerAsOfBalanceRepository extends JpaRepository<JournalLine, Long> {

    @Query("""
        SELECT
            acc.id AS accountId,
            acc.accountId AS accountCode,
            acc.accountName AS accountName,
            coa.accountType AS accountType,
            coa.normalBalance AS normalBalance,
            COALESCE(SUM(jl.debitAmount), 0) AS totalDebit,
            COALESCE(SUM(jl.creditAmount), 0) AS totalCredit
        FROM JournalLine jl
            JOIN jl.journalEntry je
            JOIN jl.account acc
            JOIN acc.coaClearTo clearTo
            JOIN clearTo.chartOfAccount coa
        WHERE
            je.status = JournalStatus.POSTED
            AND je.journalDate <= :asOfDate
            AND coa.accountType IN :accountTypes
        GROUP BY
            acc.id, acc.accountId, acc.accountName, coa.accountType, coa.normalBalance
        ORDER BY
            acc.accountId ASC
        """)
    List<ProfitLossAccountProjection> findAsOfBalances(
            @Param("asOfDate") @NonNull LocalDate asOfDate,
            @Param("accountTypes") List<AccountType> accountTypes
    );
}
