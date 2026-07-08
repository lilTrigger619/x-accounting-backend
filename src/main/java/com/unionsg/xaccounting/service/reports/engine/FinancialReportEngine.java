package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.dto.reports.FinancialReportEngineRequestDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;

public interface FinancialReportEngine {
    FinancialReportTreeResponseDto generate(FinancialReportEngineRequestDto request);
}

