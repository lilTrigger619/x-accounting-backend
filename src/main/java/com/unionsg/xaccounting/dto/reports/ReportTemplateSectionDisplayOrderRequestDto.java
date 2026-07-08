package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;

public record ReportTemplateSectionDisplayOrderRequestDto(
        @NotNull Long sectionId,
        @NotNull Long version,

        @NotNull Integer displayOrder
) {}

