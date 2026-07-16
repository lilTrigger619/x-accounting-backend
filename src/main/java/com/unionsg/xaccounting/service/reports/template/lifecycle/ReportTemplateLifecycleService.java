package com.unionsg.xaccounting.service.reports.template.lifecycle;

import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;

import java.time.LocalDate;

public interface ReportTemplateLifecycleService {

    FinancialReportTreeResponseDto preview(Long templateId, LocalDate fromDate, LocalDate toDate);

    void validate(Long templateId);

    void publish(Long templateId, String updatedBy);

    void archive(Long templateId, String updatedBy);

    com.unionsg.xaccounting.dto.reports.ReportTemplateDto clone(Long templateId, String updatedBy);
}


