package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.ReportTemplateStatus;

import java.time.LocalDateTime;

public record ReportVersionDto(
        Long id,
        Integer version,
        ReportTemplateStatus status,
        String publishedBy,
        LocalDateTime publishedAt
) {
}

