package com.unionsg.xaccounting.dto.reports;

import java.util.List;

public record DesignerSectionAccountListResponseDto(
        List<ReportTemplateSectionAccountListItemDto> content
) {}

