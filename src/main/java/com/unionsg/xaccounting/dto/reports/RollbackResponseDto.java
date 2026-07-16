package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.ReportTemplateStatus;

import java.time.LocalDateTime;

public record RollbackResponseDto(
        Long draftId,
        Integer draftVersion,
        ReportTemplateStatus status,
        String createdBy,
        LocalDateTime createdDate
) {
}

