package com.unionsg.xaccounting.repository.branch;

import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** Ledger sums by branch and cost center for the branch accounting reports. */
public interface BranchLedgerRepository extends JpaRepository<JournalLine, Long> {

    interface InterBranchBalance {
        Long getBranchId();
        Long getCounterpartyBranchId();
        BigDecimal getNet();
    }

    interface CostCenterActivity {
        Long getAccountId();
        String getAccountCode();
        String getAccountName();
        AccountType getAccountType();
        Long getCostCenterId();
        BigDecimal getTotalDebit();
        BigDecimal getTotalCredit();
    }

    /** Net (debit - credit) on the inter-branch account per branch and counterparty, as of a date. */
    @Query("""
        SELECT b.id AS branchId, cp.id AS counterpartyBranchId,
               COALESCE(SUM(jl.debitAmount - jl.creditAmount), 0) AS net
        FROM JournalLine jl
            JOIN jl.journalEntry je
            LEFT JOIN jl.branch b
            LEFT JOIN jl.counterpartyBranch cp
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.deleted = false
          AND je.journalDate <= :asOf
          AND jl.account.id = :accountId
        GROUP BY b.id, cp.id
        """)
    List<InterBranchBalance> interBranchBalances(@Param("accountId") Long accountId, @Param("asOf") LocalDate asOf);

    /**
     * Period activity on income and expense accounts per cost center. Reports leave closing
     * journals out; year-end closing includes them so a year already closed nets to zero.
     */
    @Query("""
        SELECT acc.id AS accountId, acc.accountId AS accountCode, acc.accountName AS accountName,
               coa.accountType AS accountType, cc.id AS costCenterId,
               COALESCE(SUM(jl.debitAmount), 0) AS totalDebit,
               COALESCE(SUM(jl.creditAmount), 0) AS totalCredit
        FROM JournalLine jl
            JOIN jl.journalEntry je
            JOIN jl.account acc
            JOIN acc.coaClearTo clearTo
            JOIN clearTo.chartOfAccount coa
            JOIN jl.costCenter cc
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.deleted = false
          AND (:includeClosing = true OR je.journalType <> com.unionsg.xaccounting.enums.JournalType.CLOSING)
          AND je.journalDate BETWEEN :from AND :to
          AND coa.accountType IN (com.unionsg.xaccounting.enums.AccountType.INCOME,
                                  com.unionsg.xaccounting.enums.AccountType.EXPENSE)
          AND cc.id IN :costCenterIds
        GROUP BY acc.id, acc.accountId, acc.accountName, coa.accountType, cc.id
        ORDER BY acc.accountId ASC
        """)
    List<CostCenterActivity> costCenterActivity(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                                @Param("costCenterIds") Collection<Long> costCenterIds,
                                                @Param("includeClosing") boolean includeClosing);
}
