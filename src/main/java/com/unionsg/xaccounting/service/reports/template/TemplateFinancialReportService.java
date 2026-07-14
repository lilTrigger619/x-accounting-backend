package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import java.time.LocalDate;


public interface TemplateFinancialReportService {

    com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto generatePublishedReport(
            String templateCode,
            LocalDate fromDate,
            LocalDate toDate
    );

    com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto previewTemplate(
            Long templateId,
            LocalDate fromDate,
            LocalDate toDate
    );
}

