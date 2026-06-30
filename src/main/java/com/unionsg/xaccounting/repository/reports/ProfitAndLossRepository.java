package com.unionsg.xaccounting.repository.reports;
//import com.yourcompany.accounting.chart.enums.AccountType;
//import com.yourcompany.accounting.journal.entity.JournalLine;
//import com.yourcompany.accounting.report.projection.ProfitLossAccountProjection;
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

public interface ProfitAndLossRepository extends JpaRepository<JournalLine, Long> {

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

            AND je.journalDate BETWEEN :fromDate AND :toDate

            AND coa.accountType IN :accountTypes

        GROUP BY

            acc.id,

            acc.accountId,

            acc.accountName,

            coa.accountType,

            coa.normalBalance

        ORDER BY

            acc.accountName ASC

        """)
    List<ProfitLossAccountProjection> findProfitLossAccounts(

            @Param("fromDate")
            @NonNull
            LocalDate fromDate,

            @Param("toDate")
            @NonNull
            LocalDate toDate,

            @Param("accountTypes")
            List<AccountType> accountTypes

    );

}