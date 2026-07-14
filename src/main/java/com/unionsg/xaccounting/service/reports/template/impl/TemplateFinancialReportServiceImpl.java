package com.unionsg.xaccounting.service.reports.template.impl;

import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;

import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;
import com.unionsg.xaccounting.service.reports.engine.FinancialReportEngine;
import com.unionsg.xaccounting.service.reports.template.TemplateFinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;


@Service
@RequiredArgsConstructor
public class TemplateFinancialReportServiceImpl implements TemplateFinancialReportService {

    private final ReportTemplateRepository reportTemplateRepository;
    private final FinancialReportEngine financialReportEngine;

    @Override
    public FinancialReportTreeResponseDto generatePublishedReport(
            String templateCode,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        ReportTemplate template = reportTemplateRepository
                .findTopByTemplateCodeAndStatusOrderByVersionDesc(templateCode, ReportTemplateStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException("Published template not found for code: " + templateCode));

        return financialReportEngine.generateFromTemplate(template, fromDate, toDate);
    }

    @Override
    public FinancialReportTreeResponseDto previewTemplate(
            Long templateId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        ReportTemplate template = reportTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found for id: " + templateId));

        if (template.getStatus() != ReportTemplateStatus.DRAFT && template.getStatus() != ReportTemplateStatus.PUBLISHED) {
            throw new IllegalArgumentException("Template preview not allowed for status: " + template.getStatus());
        }

        return financialReportEngine.generateFromTemplate(template, fromDate, toDate);
    }
}

