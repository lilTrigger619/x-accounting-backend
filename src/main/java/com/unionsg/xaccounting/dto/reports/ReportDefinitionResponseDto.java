package com.unionsg.xaccounting.dto.reports;

public record ReportDefinitionResponseDto(
        Long id,
        String code,
        String name,
        String description,
        boolean active,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
) {
}

