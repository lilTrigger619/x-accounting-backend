package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.service.reports.template.*;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateDraftLockEnforcer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportTemplateDraftLockEnforcerImpl implements ReportTemplateDraftLockEnforcer {

    private final ReportTemplateDraftLockValidator validator;

    @Override
    public void assertLocked(Long templateId, ReportTemplateDraftLockContext context) {
        validator.assertCanEdit(templateId, context.lockedBy(), context.editSessionId());
    }
}

