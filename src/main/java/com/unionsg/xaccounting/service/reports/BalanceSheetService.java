package com.unionsg.xaccounting.service.reports;

import com.unionsg.xaccounting.dto.reports.BalanceSheetResponseDTO;

import java.time.LocalDate;

public interface BalanceSheetService {

    BalanceSheetResponseDTO generateReport(LocalDate asOfDate);

    /** costCenterIds null = every cost center (consolidated). */
    BalanceSheetResponseDTO generateReport(LocalDate asOfDate, java.util.Set<Long> costCenterIds);

}
