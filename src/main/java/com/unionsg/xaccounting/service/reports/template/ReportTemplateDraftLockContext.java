package com.unionsg.xaccounting.service.reports.template;

/**
 * Extensible context for enforcing draft locks on write operations.
 */
public record ReportTemplateDraftLockContext(
        String lockedBy,
        String editSessionId
) {
}

