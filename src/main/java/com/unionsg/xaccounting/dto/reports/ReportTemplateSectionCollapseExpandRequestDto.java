package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;

public record ReportTemplateSectionCollapseExpandRequestDto(
        @NotNull Long sectionId,
        @NotNull Long version,

        boolean collapsed
) {}

