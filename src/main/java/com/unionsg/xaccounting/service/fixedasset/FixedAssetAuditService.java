package com.unionsg.xaccounting.service.fixedasset;

import com.unionsg.xaccounting.entity.fixedasset.FixedAssetAuditLog;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetAuditAction;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetAuditLogRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes the fixed asset audit log. Joins the caller's transaction, so a failed action leaves no entry. */
@Service
@RequiredArgsConstructor
public class FixedAssetAuditService {

    private final FixedAssetAuditLogRepository repository;

    @Transactional
    public void recordAsset(Long assetId, FixedAssetAuditAction action, Enum<?> previousStatus, Enum<?> newStatus, String details) {
        record(assetId, null, action, previousStatus, newStatus, details);
    }

    @Transactional
    public void recordRun(Long runId, FixedAssetAuditAction action, Enum<?> previousStatus, Enum<?> newStatus, String details) {
        record(null, runId, action, previousStatus, newStatus, details);
    }

    @Transactional
    public void record(Long assetId, Long runId, FixedAssetAuditAction action, Enum<?> previousStatus, Enum<?> newStatus, String details) {
        FixedAssetAuditLog log = new FixedAssetAuditLog();
        log.setAssetId(assetId);
        log.setRunId(runId);
        log.setAction(action);
        log.setPreviousStatus(previousStatus != null ? previousStatus.name() : null);
        log.setNewStatus(newStatus != null ? newStatus.name() : null);
        log.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
        log.setPerformedBy(SecurityUtils.getCurrentUser());
        repository.save(log);
    }
}
