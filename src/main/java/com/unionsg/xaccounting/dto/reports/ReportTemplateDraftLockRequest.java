package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReportTemplateDraftLockRequest(
        @NotNull
        Long templateId,
        @NotBlank
        String lockedBy,
        @NotBlank
        String editSessionId,
        long ttlSeconds
) {
}

