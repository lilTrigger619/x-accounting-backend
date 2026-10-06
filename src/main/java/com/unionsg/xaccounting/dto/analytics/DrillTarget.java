package com.unionsg.xaccounting.dto.analytics;

import java.util.List;

/** Where clicking a figure leads: the accounts behind it, over a period or as a balance at a date. */
public record DrillTarget(String label, List<String> classes, List<Long> accountIds, String basis) {
}
