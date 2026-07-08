package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;

public record ReportTemplateSectionDeleteRequestDto(
        @NotNull Long sectionId,
        @NotNull Long version
) {}

