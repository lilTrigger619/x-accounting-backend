package com.unionsg.xaccounting.dto.accounting;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateFinancialYearRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    /** Defaults to true: divide the year into consecutive monthly AccountingPeriods. */
    private Boolean generateMonthlyPeriods;
}
