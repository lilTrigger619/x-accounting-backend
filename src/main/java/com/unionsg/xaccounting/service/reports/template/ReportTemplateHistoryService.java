package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.dto.reports.ReportTemplateHistoryDto;

import java.util.List;

public interface ReportTemplateHistoryService {

    List<ReportTemplateHistoryDto> getHistory(Long templateId);
}

