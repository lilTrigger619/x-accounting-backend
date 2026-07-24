package com.unionsg.xaccounting.repository.journal;

import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.enums.JournalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    Optional<JournalEntry> findByJournalNumber(String journalNumber);

    boolean existsByJournalNumber(String journalNumber);

    long countByJournalDateBetween(LocalDate start, LocalDate end);

    long countByStatus(JournalStatus status);

    Optional<JournalEntry> findBySourceModuleAndSourceEntityIdAndStatus(
            String sourceModule, Long sourceEntityId, JournalStatus status
    );
}
