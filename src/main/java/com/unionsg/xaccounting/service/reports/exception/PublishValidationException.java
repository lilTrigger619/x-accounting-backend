package com.unionsg.xaccounting.service.reports.exception;

import java.util.List;

public class PublishValidationException extends RuntimeException {

    private final List<ValidationError> errors;

    public PublishValidationException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public record ValidationError(String code, String message, String path) {}
}

