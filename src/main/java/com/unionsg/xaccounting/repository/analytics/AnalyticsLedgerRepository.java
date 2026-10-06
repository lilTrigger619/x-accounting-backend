package com.unionsg.xaccounting.repository.analytics;

import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Ledger reads for the Business Intelligence module. Every query counts journals that are
 * POSTED or REVERSED: a reversed journal and the posted journal that reverses it cancel each
 * other out, so counting only POSTED would leave the reversal's opposite entry on its own.
 * Drafts never count.
 */
public interface AnalyticsLedgerRepository extends JpaRepository<JournalLine, Long> {

    /** Every account that has ever been posted to, with its classification inputs. */
    @Query("""
        SELECT acc.id AS accountId, acc.accountId AS accountCode, acc.accountName AS accountName,
               coa.accountType AS accountType, coa.coaCode AS chartCode, coa.coa_description AS chartName,
               clearTo.clearToCode AS clearToCode, clearTo.description AS clearToName
        FROM AccountEntity acc
            JOIN acc.coaClearTo clearTo
            JOIN clearTo.chartOfAccount coa
        """)
    List<AnalyticsAccountRow> findAllAccounts();

    /** Activity per account per day inside [from, to]. */
    @Query("""
        SELECT acc.id AS accountId, je.journalDate AS day,
               COALESCE(SUM(jl.debitAmount), 0) AS debit, COALESCE(SUM(jl.creditAmount), 0) AS credit
        FROM JournalLine jl
            JOIN jl.journalEntry je
            JOIN jl.account acc
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.journalDate >= :from AND je.journalDate <= :to
        GROUP BY acc.id, je.journalDate
        """)
    List<AnalyticsDailyRow> findDailyActivity(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Cumulative balance per account for everything dated before {@code before}. */
    @Query("""
        SELECT acc.id AS accountId,
               COALESCE(SUM(jl.debitAmount), 0) AS debit, COALESCE(SUM(jl.creditAmount), 0) AS credit
        FROM JournalLine jl
            JOIN jl.journalEntry je
            JOIN jl.account acc
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.journalDate < :before
        GROUP BY acc.id
        """)
    List<AnalyticsBalanceRow> findBalancesBefore(@Param("before") LocalDate before);

    /**
     * Activity on the given accounts per day, split by the invoice each journal came from. A
     * reversal journal carries no source of its own, so it inherits the source of the journal it
     * reverses. {@code invoiceId} is null for journals that did not come from an invoice.
     */
    @Query("""
        SELECT acc.id AS accountId, je.journalDate AS day,
               CASE WHEN je.sourceModule = 'INVOICE' THEN je.sourceEntityId
                    WHEN orig.sourceModule = 'INVOICE' THEN orig.sourceEntityId
                    ELSE NULL END AS invoiceId,
               COALESCE(SUM(jl.debitAmount), 0) AS debit, COALESCE(SUM(jl.creditAmount), 0) AS credit
        FROM JournalLine jl
            JOIN jl.journalEntry je
            JOIN jl.account acc
            LEFT JOIN JournalEntry orig ON orig.id = je.reversalOfJournalId
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.journalDate >= :from AND je.journalDate <= :to
          AND acc.id IN :accountIds
        GROUP BY acc.id, je.journalDate,
               CASE WHEN je.sourceModule = 'INVOICE' THEN je.sourceEntityId
                    WHEN orig.sourceModule = 'INVOICE' THEN orig.sourceEntityId
                    ELSE NULL END
        """)
    List<AnalyticsInvoiceActivityRow> findActivityByInvoice(@Param("from") LocalDate from,
                                                            @Param("to") LocalDate to,
                                                            @Param("accountIds") Collection<Long> accountIds);

    @Query("""
        SELECT DISTINCT i FROM Invoice i
            LEFT JOIN FETCH i.items it
            LEFT JOIN FETCH it.product
            LEFT JOIN FETCH i.customer
        WHERE i.id IN :ids
        """)
    List<Invoice> findInvoicesWithItems(@Param("ids") Collection<Long> ids);

    /** Journal lines behind a drill-down, newest first. */
    @Query("""
        SELECT jl.id AS lineId, je.id AS journalId, je.journalNumber AS journalNumber, je.journalDate AS journalDate,
               je.status AS status, je.description AS journalDescription, jl.description AS lineDescription,
               je.reference AS reference, je.sourceModule AS sourceModule, je.sourceEntityId AS sourceEntityId,
               orig.sourceModule AS originalSourceModule, orig.sourceEntityId AS originalSourceEntityId,
               je.reversalOfJournalId AS reversalOfJournalId,
               acc.id AS accountId, acc.accountId AS accountCode, acc.accountName AS accountName,
               jl.debitAmount AS debit, jl.creditAmount AS credit
        FROM JournalLine jl
            JOIN jl.journalEntry je
            JOIN jl.account acc
            LEFT JOIN JournalEntry orig ON orig.id = je.reversalOfJournalId
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.journalDate >= :from
          AND je.journalDate <= :to
          AND acc.id IN :accountIds
        ORDER BY je.journalDate DESC, je.id DESC, jl.id ASC
        """)
    List<AnalyticsLineRow> findLines(@Param("from") LocalDate from,
                                     @Param("to") LocalDate to,
                                     @Param("accountIds") Collection<Long> accountIds,
                                     Pageable pageable);

    @Query("""
        SELECT COUNT(jl) FROM JournalLine jl
            JOIN jl.journalEntry je
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.journalDate >= :from
          AND je.journalDate <= :to
          AND jl.account.id IN :accountIds
        """)
    long countLines(@Param("from") LocalDate from, @Param("to") LocalDate to,
                    @Param("accountIds") Collection<Long> accountIds);

    /** Posted journals recorded in a currency other than the base one, where no rate was captured. */
    @Query("""
        SELECT COUNT(je) FROM JournalEntry je
        WHERE je.status IN (com.unionsg.xaccounting.enums.JournalStatus.POSTED,
                            com.unionsg.xaccounting.enums.JournalStatus.REVERSED)
          AND je.journalDate >= :from AND je.journalDate <= :to
          AND je.currencyCode IS NOT NULL AND UPPER(je.currencyCode) <> :baseCurrency
          AND (je.exchangeRate IS NULL OR je.exchangeRate = 1)
        """)
    long countForeignJournalsWithoutRate(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                         @Param("baseCurrency") String baseCurrency);

    @Query("""
        SELECT DISTINCT l.principalAccount.id FROM Loan l
        WHERE l.direction = com.unionsg.xaccounting.enums.loan.LoanDirection.BORROWED_LOAN
          AND l.principalAccount IS NOT NULL
        """)
    List<Long> findBorrowedLoanPrincipalAccountIds();

    /** Open invoices as they stand today: due date, balance and currency. */
    @Query("""
        SELECT i.id AS id, i.dueDate AS dueDate, i.balance AS balance, i.currency AS currency
        FROM Invoice i
        WHERE i.status IN (com.unionsg.xaccounting.enums.InvoiceStatus.SENT,
                           com.unionsg.xaccounting.enums.InvoiceStatus.PARTIALLY_PAID,
                           com.unionsg.xaccounting.enums.InvoiceStatus.OVERDUE)
          AND i.balance > 0
        """)
    List<AnalyticsOpenDocumentRow> findOpenInvoices();

    @Query("""
        SELECT b.id AS id, b.dueDate AS dueDate, b.balance AS balance, b.currency AS currency
        FROM Bill b
        WHERE b.status IN (com.unionsg.xaccounting.enums.BillStatus.OPEN,
                           com.unionsg.xaccounting.enums.BillStatus.PARTIALLY_PAID,
                           com.unionsg.xaccounting.enums.BillStatus.OVERDUE)
          AND b.balance > 0
        """)
    List<AnalyticsOpenDocumentRow> findOpenBills();
}
