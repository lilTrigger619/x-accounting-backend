package com.unionsg.xaccounting.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String permission) {
        super("Access Denied: Missing permission '" + permission + "'");
    }
}