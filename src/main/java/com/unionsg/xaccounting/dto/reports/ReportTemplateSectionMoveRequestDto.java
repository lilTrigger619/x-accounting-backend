package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;

public record ReportTemplateSectionMoveRequestDto(
        @NotNull Long sectionId,
        @NotNull Long version,

        Long newParentSectionId,

        @NotNull Integer newDisplayOrder
) {}

