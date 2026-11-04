package com.unionsg.xaccounting.exception;

public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String permission) {
        super("Access Denied: Missing permission '" + permission + "'");
    }

    private AccessDeniedException(String message, boolean ignored) {
        super(message);
    }

    /** Refused because the record lies outside the user's branch / cost center scope. */
    public static AccessDeniedException outOfScope(String message) {
        return new AccessDeniedException(message, true);
    }
}
