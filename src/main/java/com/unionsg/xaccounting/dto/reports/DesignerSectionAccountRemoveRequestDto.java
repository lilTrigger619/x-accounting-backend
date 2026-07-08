package com.unionsg.xaccounting.dto.reports;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DesignerSectionAccountRemoveRequestDto(
        @NotNull Long reportTemplateSectionId,
        @NotNull List<Long> accountIds
) {
}

