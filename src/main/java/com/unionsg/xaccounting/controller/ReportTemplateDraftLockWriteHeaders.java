package com.unionsg.xaccounting.controller;

/**
 * Request headers expected from the frontend for draft write operations.
 */
public final class ReportTemplateDraftLockWriteHeaders {

    private ReportTemplateDraftLockWriteHeaders() {}

    public static final String LOCKED_BY = "X-Edit-By";
    public static final String EDIT_SESSION_ID = "X-Edit-Session-Id";

    public static final String DEFAULT_TTL_SECONDS_PROP = "report.draftLock.ttlSeconds";
}

