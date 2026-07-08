package com.unionsg.xaccounting.service.reports.exception;

public class TemplateSectionCycleDetectedException extends RuntimeException {
    public TemplateSectionCycleDetectedException(String message) {
        super(message);
    }
}

