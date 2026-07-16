package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.repository.reports.ReportTemplateDraftLockRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Transactional
public class ReportTemplateDraftLockCleanupJob {

    private final ReportTemplateDraftLockRepository lockRepository;

    @Scheduled(fixedDelayString = "${report.draftLock.cleanup.fixedDelayMs:60000}")
    public void cleanupExpiredLocks() {
        lockRepository.deleteExpired(LocalDateTime.now());
    }
}

