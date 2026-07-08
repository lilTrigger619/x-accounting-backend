package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.ReportTemplateStatus;

import java.time.LocalDateTime;

public record ReportTemplateResponseDto(
        Long id,
        String templateCode,
        String templateName,
        String description,
        String category,
        ReportTemplateStatus status,
        Integer version,
        boolean isSystemTemplate,
        String createdBy,
        LocalDateTime createdDate,
        String updatedBy,
        LocalDateTime updatedDate
) {
}

