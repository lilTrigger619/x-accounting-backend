package com.unionsg.xaccounting.service.reports;


import com.unionsg.xaccounting.dto.reports.ProfitLossReportResponseDTO;

import java.time.LocalDate;

public interface ProfitAndLossService {

    ProfitLossReportResponseDTO generateReport(
            LocalDate fromDate,
            LocalDate toDate
    );

}