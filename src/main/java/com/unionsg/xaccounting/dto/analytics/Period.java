package com.unionsg.xaccounting.dto.analytics;

import java.time.LocalDate;

public record Period(LocalDate from, LocalDate to, String label) {
}
