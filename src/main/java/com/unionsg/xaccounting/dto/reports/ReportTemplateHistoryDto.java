package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.ReportTemplateHistoryAction;

import java.time.LocalDateTime;

public record ReportTemplateHistoryDto(
        Long id,
        Long templateId,
        ReportTemplateHistoryAction action,
        String performedBy,
        LocalDateTime performedAt,
        String metadata
) {
}

