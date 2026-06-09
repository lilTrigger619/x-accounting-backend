package com.unionsg.xaccounting.repository.journal;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {

    List<JournalLine> findByJournalEntryId(Long journalEntryId);

    List<JournalLine> findByAccountId(Long accountId);
}