package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportDefinitionRequestDto(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 1000) String description,
        boolean active
) {
}

