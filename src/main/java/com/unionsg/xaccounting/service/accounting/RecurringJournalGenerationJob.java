package com.unionsg.xaccounting.service.accounting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringJournalGenerationJob {

    private final RecurringJournalService recurringJournalService;

    @Scheduled(cron = "${accounting.recurringJournal.cron:0 0 1 * * *}")
    public void run() {
        log.info("Running scheduled recurring journal generation");
        recurringJournalService.generateDueOccurrences();
    }
}
