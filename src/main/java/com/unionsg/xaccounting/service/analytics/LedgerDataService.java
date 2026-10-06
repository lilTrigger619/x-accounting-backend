package com.unionsg.xaccounting.service.analytics;

import com.unionsg.xaccounting.repository.analytics.AnalyticsLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LedgerDataService {

    private final AnalyticsLedgerRepository ledgerRepository;
    private final AccountClassifier classifier;

    /** Loads opening balances at {@code from} and daily activity through {@code to}. */
    @Transactional(readOnly = true)
    public LedgerData load(LocalDate from, LocalDate to) {
        return new LedgerData(classifier.classifyAll(), from,
                ledgerRepository.findBalancesBefore(from),
                ledgerRepository.findDailyActivity(from, to));
    }
}
