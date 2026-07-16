package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportTemplateRequestDto(
        @NotBlank
        @Size(max = 100)
        String templateCode,

        @NotBlank
        @Size(max = 200)
        String templateName,

        @Size(max = 1000)
        String description,

        @Size(max = 100)
        String category,

        @NotNull
        ReportTemplateStatus status,

        @NotNull
        Integer version,

        boolean isSystemTemplate

) {
}


