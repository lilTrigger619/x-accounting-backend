package com.unionsg.xaccounting.dto.reports;

import com.unionsg.xaccounting.enums.SectionType;

import java.math.BigDecimal;
import java.util.List;

public record FinancialReportTreeNodeDto(
        Long sectionId,
        String code,
        String title,
        SectionType sectionType,
        BigDecimal value,
        List<FinancialReportTreeNodeDto> children
) {

    public static FinancialReportTreeNodeDto ofLeaf(Long sectionId,
                                                       String code,
                                                       String title,
                                                       SectionType sectionType,
                                                       BigDecimal value) {
        return new FinancialReportTreeNodeDto(sectionId, code, title, sectionType, value, List.of());
    }
}

