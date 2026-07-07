package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.SectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportSectionRequestDto(
        @NotNull Long reportDefinitionId,
        Long parentSectionId,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 100) String code,
        @NotNull Integer displayOrder,
        @NotNull SectionType sectionType,
        String formula,
        boolean active
) {
}

