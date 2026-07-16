package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.dto.reports.ReportTemplateDraftLockDto;

import com.unionsg.xaccounting.entity.reports.ReportTemplateDraftLock;

import jakarta.persistence.LockTimeoutException;

import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.repository.reports.ReportTemplateDraftLockRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.service.reports.exception.DraftLockOwnedException;
import com.unionsg.xaccounting.service.reports.exception.InvalidTemplateStateException;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateDraftLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportTemplateDraftLockServiceImpl implements ReportTemplateDraftLockService {

    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateDraftLockRepository lockRepository;

    @Override
    @Transactional
    public ReportTemplateDraftLockDto lock(Long templateId, String lockedBy, String editSessionId, long ttlSeconds) {
        var template = templateRepository.findById(templateId)
                .orElseThrow(() -> new InvalidTemplateStateException("Template not found for id=" + templateId));

        if (template.getStatus() != ReportTemplateStatus.DRAFT) {
            throw new InvalidTemplateStateException("Only DRAFT templates can be locked. Current=" + template.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        lockRepository.deleteExpired(now);

        ReportTemplateDraftLock existing = lockRepository.findByTemplateId(templateId).orElse(null);
        LocalDateTime newExpires = now.plusSeconds(ttlSeconds);

        if (existing == null) {
            ReportTemplateDraftLock created = ReportTemplateDraftLock.builder()
                    .templateId(templateId)
                    .editSessionId(editSessionId)
                    .lockedBy(lockedBy)
                    .lockedAt(now)
                    .lastHeartbeat(now)
                    .expiresAt(newExpires)
                    .build();
            return toDto(lockRepository.save(created));
        }

        if (existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now)) {
            // active lock
            if (!existing.getEditSessionId().equals(editSessionId)) {
                throw new DraftLockOwnedException(
                        "Report template is currently being edited by another session",
                        existing.getLockedBy()
                );
            }

            // same session: treat as heartbeat
            existing.setLockedBy(lockedBy); // keep last caller info
            existing.setLastHeartbeat(now);
            existing.setExpiresAt(newExpires);
            existing.setEditSessionId(editSessionId);
            return toDto(lockRepository.save(existing));
        }

        // expired: replace
        lockRepository.deleteByTemplateId(templateId);
        ReportTemplateDraftLock created = ReportTemplateDraftLock.builder()
                .templateId(templateId)
                .editSessionId(editSessionId)
                .lockedBy(lockedBy)
                .lockedAt(now)
                .lastHeartbeat(now)
                .expiresAt(newExpires)
                .build();
        return toDto(lockRepository.save(created));
    }

    @Override
    @Transactional
    public ReportTemplateDraftLockDto heartbeat(Long templateId, String lockedBy, String editSessionId, long ttlSeconds) {
        // current implementation treats heartbeat as the same as lock with same editSessionId
        return lock(templateId, lockedBy, editSessionId, ttlSeconds);
    }

    @Override
    @Transactional
    public void unlock(Long templateId, String lockedBy, String editSessionId) {
        LocalDateTime now = LocalDateTime.now();
        lockRepository.deleteExpired(now);

        ReportTemplateDraftLock existing = lockRepository.findByTemplateId(templateId).orElse(null);
        if (existing == null) return;

        if (existing.getExpiresAt() != null && existing.getExpiresAt().isAfter(now)) {
            // active lock must match session and (optional) user
            if (editSessionId != null && !editSessionId.equals(existing.getEditSessionId())) {
                throw new DraftLockOwnedException("Cannot unlock: lock owned by another session", existing.getLockedBy());
            }
            if (lockedBy != null && !lockedBy.equals(existing.getLockedBy())) {
                throw new DraftLockOwnedException("Cannot unlock: lock owned by another user", existing.getLockedBy());
            }
        }

        lockRepository.deleteByTemplateId(templateId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportTemplateDraftLockDto get(Long templateId) {
        LocalDateTime now = LocalDateTime.now();
        lockRepository.deleteExpired(now);
        return lockRepository.findByTemplateId(templateId).map(this::toDto).orElse(null);
    }

    private ReportTemplateDraftLockDto toDto(ReportTemplateDraftLock l) {
        if (l == null) return null;
        return new ReportTemplateDraftLockDto(
                l.getTemplateId(),
                l.getEditSessionId(),
                l.getLockedBy(),
                l.getLockedAt(),
                l.getLastHeartbeat(),
                l.getExpiresAt()
        );
    }
}

