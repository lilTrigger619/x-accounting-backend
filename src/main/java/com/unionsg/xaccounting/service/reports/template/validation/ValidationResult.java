package com.unionsg.xaccounting.service.reports.template.validation;

import com.unionsg.xaccounting.dto.reports.ReportTemplateValidationResponse;
import com.unionsg.xaccounting.dto.reports.ValidationErrorDto;
import com.unionsg.xaccounting.dto.reports.ValidationWarningDto;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final List<ValidationErrorDto> errors = new ArrayList<>();
    private final List<ValidationWarningDto> warnings = new ArrayList<>();

    public void addError(ValidationErrorDto error) {
        if (error != null) errors.add(error);
    }

    public void addWarning(ValidationWarningDto warning) {
        if (warning != null) warnings.add(warning);
    }

    public List<ValidationErrorDto> getErrors() {
        return errors;
    }

    public List<ValidationWarningDto> getWarnings() {
        return warnings;
    }

    public ReportTemplateValidationResponse toResponse() {
        return ReportTemplateValidationResponse.of(errors, warnings);
    }
}

