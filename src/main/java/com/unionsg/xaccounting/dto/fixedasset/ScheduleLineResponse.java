package com.unionsg.xaccounting.dto.fixedasset;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/**
 * One row of an asset's depreciation schedule. {@code status} is POSTED or REVERSED for charges from
 * a run, CALCULATED for a charge sitting in a run not yet posted, and PROJECTED for future months.
 */
@Getter
@Setter
public class ScheduleLineResponse {
    private String status;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String periodName;
    private BigDecimal openingCarryingAmount;
    private BigDecimal depreciationAmount;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal closingCarryingAmount;
    private BigDecimal monthsCharged;
    private Long runId;
    private String runNumber;
    private Long journalId;
    private String journalNumber;
    private String explanation;
}
