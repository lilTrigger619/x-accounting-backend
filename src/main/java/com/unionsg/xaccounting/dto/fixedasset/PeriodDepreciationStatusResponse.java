package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

/** An accounting period with where its depreciation stands; doubles as the period picker. */
@Getter
@Setter
public class PeriodDepreciationStatusResponse {
    private Long periodId;
    private String periodName;
    private Long financialYearId;
    private String financialYearName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodStatus;
    private Boolean postable;
    private Long runId;
    private String runNumber;
    private DepreciationRunStatus runStatus;
    private BigDecimal totalDepreciation;
}
