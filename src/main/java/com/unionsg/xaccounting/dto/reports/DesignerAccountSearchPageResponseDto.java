package com.unionsg.xaccounting.dto.reports;

import java.util.List;

/**
 * Lightweight paging response for designer account pickers.
 */
public record DesignerAccountSearchPageResponseDto(
        List<DesignerAccountListItemDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {}

