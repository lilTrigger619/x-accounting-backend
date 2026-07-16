package com.unionsg.xaccounting.service.reports.template;

/**
 * Enforces draft lock ownership for write operations.
 */
public interface ReportTemplateDraftLockEnforcer {

    void assertLocked(Long templateId, ReportTemplateDraftLockContext context);
}

