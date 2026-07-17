package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.ReportTemplateValidationSeverity;

public record ValidationWarningDto(
        String code,
        String message,
        String sectionCode,
        String path,
        ReportTemplateValidationSeverity severity
) {}

