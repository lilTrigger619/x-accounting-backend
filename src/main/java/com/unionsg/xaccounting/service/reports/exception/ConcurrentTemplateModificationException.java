package com.unionsg.xaccounting.service.reports.exception;

public class ConcurrentTemplateModificationException extends RuntimeException {
    public ConcurrentTemplateModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

