package com.unionsg.xaccounting.dto.reports;

import java.time.LocalDateTime;

public record ReportTemplateDraftLockDto(
        Long templateId,
        String editSessionId,
        String lockedBy,
        LocalDateTime lockedAt,
        LocalDateTime lastHeartbeat,
        LocalDateTime expiresAt
) {

    public boolean isActive() {
        return expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
}

