package com.unionsg.xaccounting.service.reports;


import com.unionsg.xaccounting.dto.reports.ProfitLossReportResponseDTO;

import java.time.LocalDate;

public interface ProfitAndLossFormatter {

    ProfitLossReportResponseDTO format(
            LocalDate fromDate,
            LocalDate toDate
    );

}