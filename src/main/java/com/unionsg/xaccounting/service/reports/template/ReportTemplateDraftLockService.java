package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.dto.reports.ReportTemplateDraftLockDto;

public interface ReportTemplateDraftLockService {

    ReportTemplateDraftLockDto lock(Long templateId, String lockedBy, String editSessionId, long ttlSeconds);

    ReportTemplateDraftLockDto heartbeat(Long templateId, String lockedBy, String editSessionId, long ttlSeconds);

    void unlock(Long templateId, String lockedBy, String editSessionId);

    ReportTemplateDraftLockDto get(Long templateId);
}

