package com.unionsg.xaccounting.repository.accounting;

import com.unionsg.xaccounting.entity.accounting.RecurringJournalOccurrence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurringJournalOccurrenceRepository extends JpaRepository<RecurringJournalOccurrence, Long> {

    List<RecurringJournalOccurrence> findByTemplateIdOrderByScheduledDateDesc(Long templateId);

    Optional<RecurringJournalOccurrence> findByTemplateIdAndScheduledDate(Long templateId, LocalDate scheduledDate);
}
