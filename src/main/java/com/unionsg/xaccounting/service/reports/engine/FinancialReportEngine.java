package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.dto.reports.FinancialReportEngineRequestDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;

public interface FinancialReportEngine {
    FinancialReportTreeResponseDto generate(FinancialReportEngineRequestDto request);

    FinancialReportTreeResponseDto generateFromTemplate(
            com.unionsg.xaccounting.entity.reports.ReportTemplate template,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate
    );
}



