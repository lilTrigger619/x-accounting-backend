package com.unionsg.xaccounting.dto.reports;

import java.util.List;

public record ReportTemplateValidationResponse(
        boolean valid,
        List<ValidationErrorDto> errors,
        List<ValidationWarningDto> warnings
) {

    public static ReportTemplateValidationResponse of(
            List<ValidationErrorDto> errors,
            List<ValidationWarningDto> warnings
    ) {
        boolean valid = errors == null || errors.isEmpty();
        return new ReportTemplateValidationResponse(
                valid,
                errors == null ? List.of() : errors,
                warnings == null ? List.of() : warnings
        );
    }
}

