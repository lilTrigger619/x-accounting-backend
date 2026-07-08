package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;

public record ReportTemplateSectionAccountRequestDto(
        @NotNull
        Long reportTemplateSectionId,

        @NotNull
        Long accountId,

        @NotNull
        Integer displayOrder
) {
}

