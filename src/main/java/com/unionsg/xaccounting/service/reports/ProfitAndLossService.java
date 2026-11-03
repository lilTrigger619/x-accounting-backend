package com.unionsg.xaccounting.service.reports;


import com.unionsg.xaccounting.dto.reports.ProfitLossReportInternalDTO;
import com.unionsg.xaccounting.dto.reports.ProfitLossReportResponseDTO;

import java.time.LocalDate;

public interface ProfitAndLossService {

    ProfitLossReportInternalDTO generateReport(
            LocalDate fromDate,
            LocalDate toDate
    );

    /** costCenterIds null = every cost center (consolidated). */
    ProfitLossReportInternalDTO generateReport(
            LocalDate fromDate,
            LocalDate toDate,
            java.util.Set<Long> costCenterIds
    );

}