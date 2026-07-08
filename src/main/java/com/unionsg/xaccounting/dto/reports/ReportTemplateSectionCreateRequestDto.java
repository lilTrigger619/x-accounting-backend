package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.SectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportTemplateSectionCreateRequestDto(
        @NotBlank
        @Size(max = 200)
        String sectionCode,

        @NotBlank
        @Size(max = 200)
        String title,

        @NotNull
        Integer displayOrder,

        @NotNull
        SectionType sectionType,

        String formula,

        boolean visible,

        boolean expandedByDefault,

        Long parentSectionId
) {}

