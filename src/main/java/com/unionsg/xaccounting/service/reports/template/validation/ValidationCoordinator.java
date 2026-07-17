package com.unionsg.xaccounting.service.reports.template.validation;

import com.unionsg.xaccounting.dto.reports.ReportTemplateValidationResponse;

public interface ValidationCoordinator {

    ReportTemplateValidationResponse validateTemplate(Long templateId);
}

