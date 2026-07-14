package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.dto.reports.FinancialReportEngineRequestDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FinancialReportEngineImpl implements FinancialReportEngine {

    private final FinancialReportBuilder financialReportBuilder;

    @Override
    public FinancialReportTreeResponseDto generate(FinancialReportEngineRequestDto request) {
        return financialReportBuilder.build(request);
    }

    @Override
    public FinancialReportTreeResponseDto generateFromTemplate(com.unionsg.xaccounting.entity.reports.ReportTemplate template,
                                                                     java.time.LocalDate fromDate,
                                                                     java.time.LocalDate toDate) {
        return financialReportBuilder.buildFromTemplate(template, fromDate, toDate);
    }
}



