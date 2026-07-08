package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportTemplateSectionDuplicateRequestDto(
        @NotNull Long sectionId,
        @NotNull Long version,

        @NotBlank
        @Size(max = 200)
        String newSectionCode
) {}

