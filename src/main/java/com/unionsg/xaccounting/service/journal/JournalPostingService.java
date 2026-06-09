package com.unionsg.xaccounting.service.journal;

import com.unionsg.xaccounting.entity.Journals.JournalEntry;

public interface JournalPostingService {

    void validateJournal(JournalEntry journal);

    void validateBalanced(JournalEntry journal);

    void validateLines(JournalEntry journal);

    void validatePostingPeriod(JournalEntry journal);

    void post(JournalEntry journal);
}
