package com.unionsg.xaccounting.service.reports.template.validation;

import com.unionsg.xaccounting.entity.reports.ReportTemplate;

public interface BusinessRuleValidator {

    void validate(ReportTemplate template, ValidationResult result);
}

