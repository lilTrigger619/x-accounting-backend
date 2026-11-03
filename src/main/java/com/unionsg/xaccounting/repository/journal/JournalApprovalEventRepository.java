package com.unionsg.xaccounting.repository.journal;

import com.unionsg.xaccounting.entity.Journals.JournalApprovalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalApprovalEventRepository extends JpaRepository<JournalApprovalEvent, Long> {
    List<JournalApprovalEvent> findByJournalEntryIdOrderByCreatedAtAsc(Long journalEntryId);
}
