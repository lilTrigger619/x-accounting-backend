package com.unionsg.xaccounting.repository.accounting;

import com.unionsg.xaccounting.entity.accounting.RecurringJournalTemplate;
import com.unionsg.xaccounting.enums.RecurringJournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringJournalTemplateRepository extends JpaRepository<RecurringJournalTemplate, Long> {

    List<RecurringJournalTemplate> findAllByOrderByCreatedAtDesc();

    List<RecurringJournalTemplate> findByStatusAndNextRunDateLessThanEqual(
            RecurringJournalStatus status, LocalDate date
    );
}
