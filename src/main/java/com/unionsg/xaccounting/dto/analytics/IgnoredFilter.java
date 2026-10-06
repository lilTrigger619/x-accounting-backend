package com.unionsg.xaccounting.dto.analytics;

/** A filter the user set that this screen or metric cannot apply, and why. */
public record IgnoredFilter(String filter, String reason) {
}
