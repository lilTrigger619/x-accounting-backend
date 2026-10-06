package com.unionsg.xaccounting.repository.journal;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.JournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;

import java.util.List;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    List<JournalLine> findByJournalEntryId(Long journalEntryId);

    List<JournalLine> findByAccountId(Long accountId);

    /**
     * Net debit-minus-credit movement on one GL account (by its code) across journals in the
     * given statuses. Pass both POSTED and REVERSED: a reversed journal and its posted
     * reversal cancel each other out, so counting only POSTED would double-count the reversal.
     */
    @Query("""
        SELECT COALESCE(SUM(l.debitAmount - l.creditAmount), 0)
        FROM JournalLine l
        WHERE l.account.accountId = :accountCode
          AND l.journalEntry.status IN :statuses
          AND l.journalEntry.deleted = false
        """)
    BigDecimal sumNetMovementByAccountCode(
            @Param("accountCode") String accountCode,
            @Param("statuses") Collection<JournalStatus> statuses
    );

    /** Net debit-minus-credit movement per GL account code, for many accounts at once. */
    @Query("""
        SELECT l.account.accountId AS accountCode, COALESCE(SUM(l.debitAmount - l.creditAmount), 0) AS netMovement
        FROM JournalLine l
        WHERE l.account.accountId IN :accountCodes
          AND l.journalEntry.status IN :statuses
          AND l.journalEntry.deleted = false
        GROUP BY l.account.accountId
        """)
    List<AccountMovement> sumNetMovementByAccountCodes(
            @Param("accountCodes") Collection<String> accountCodes,
            @Param("statuses") Collection<JournalStatus> statuses
    );

    interface AccountMovement {
        String getAccountCode();

        BigDecimal getNetMovement();
    }
}
