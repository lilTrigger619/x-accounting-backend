package com.unionsg.xaccounting.dto.analytics;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The global BI filter bar, sent as query parameters by every BI screen. Missing dates mean the
 * current financial year to date.
 */
@Data
public class AnalyticsFilter {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
    private CompareMode compare = CompareMode.PREVIOUS_PERIOD;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate compareFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate compareTo;
    private Granularity granularity;
    /** Document currency (the currency an invoice or bill was raised in). */
    private String currency;
    private List<Long> customerIds = new ArrayList<>();
    private List<Long> supplierIds = new ArrayList<>();
    private List<Long> productIds = new ArrayList<>();
    private List<Long> accountIds = new ArrayList<>();
    private Long branchId;
    private Long departmentId;
}
