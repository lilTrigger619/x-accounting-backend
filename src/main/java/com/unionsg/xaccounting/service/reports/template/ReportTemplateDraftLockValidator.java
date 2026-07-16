package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.entity.reports.ReportTemplateDraftLock;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.repository.reports.ReportTemplateDraftLockRepository;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.service.reports.exception.DraftLockOwnedException;
import com.unionsg.xaccounting.service.reports.exception.InvalidTemplateStateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReportTemplateDraftLockValidator {

    private final ReportTemplateRepository templateRepository;
    private final ReportTemplateDraftLockRepository lockRepository;

    public void assertCanEdit(Long templateId, String lockedBy, String editSessionId) {
        var template = templateRepository.findById(templateId)
                .orElseThrow(() -> new InvalidTemplateStateException("Template not found for id=" + templateId));

        if (template.getStatus() != ReportTemplateStatus.DRAFT) {
            throw new InvalidTemplateStateException("Only DRAFT templates are editable. Current=" + template.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        lockRepository.deleteExpired(now);

        ReportTemplateDraftLock lock = lockRepository.findByTemplateId(templateId).orElse(null);
        if (lock == null || lock.getExpiresAt() == null || !lock.getExpiresAt().isAfter(now)) {
            throw new DraftLockOwnedException("Report template is currently being edited by another session", null);
        }

        // For correctness with prompt, require editSessionId ownership.
        boolean sessionMatches = editSessionId != null && editSessionId.equals(lock.getEditSessionId());
        if (!sessionMatches) {
            throw new DraftLockOwnedException(
                    "Report template is currently being edited by another session",
                    lock.getLockedBy()
            );
        }

        // Enforce lockedBy too (meaningful error required by prompt)
        boolean userMatches = lockedBy != null && lockedBy.equals(lock.getLockedBy());
        if (!userMatches) {
            throw new DraftLockOwnedException(
                    "Report template is currently being edited by another user",
                    lock.getLockedBy()
            );
        }
    }

}

