package com.unionsg.xaccounting.dto.reports;

import lombok.Builder;

import java.util.Objects;

@Builder
public record ReportTemplateSectionAccountListItemDto(
        Long accountId,
        Integer displayOrder
) {

    public ReportTemplateSectionAccountListItemDto {
        Objects.requireNonNull(accountId, "accountId");
    }
}

