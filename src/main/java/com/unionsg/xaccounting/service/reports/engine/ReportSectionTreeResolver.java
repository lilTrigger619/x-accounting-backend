package com.unionsg.xaccounting.service.reports.engine;

import com.unionsg.xaccounting.dto.reports.FinancialReportTreeNodeDto;
import com.unionsg.xaccounting.entity.reports.ReportSection;
import com.unionsg.xaccounting.enums.SectionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class ReportSectionTreeResolver {

    public FinancialReportTreeNodeDto buildTree(List<ReportSection> sections,
                                                Map<String, BigDecimal> computedByCode) {
        // Create virtual root containing all top-level sections.
        List<ReportSection> roots = sections.stream()
                .filter(s -> s.getParentSection() == null)
                .sorted(Comparator.comparingInt(ReportSection::getDisplayOrder))
                .toList();

        List<FinancialReportTreeNodeDto> rootChildren = roots.stream()
                .map(s -> buildNode(s, sections, computedByCode))
                .toList();

        // Root node has no actual sectionId. Frontend can use children.
        return new FinancialReportTreeNodeDto(null, "ROOT", "ROOT", SectionType.SECTION, BigDecimal.ZERO, rootChildren);
    }

    private FinancialReportTreeNodeDto buildNode(ReportSection section,
                                                 List<ReportSection> all,
                                                 Map<String, BigDecimal> computedByCode) {

        List<ReportSection> children = all.stream()
                .filter(s -> s.getParentSection() != null && s.getParentSection().getId() != null && s.getParentSection().getId().equals(section.getId()))
                .sorted(Comparator.comparingInt(ReportSection::getDisplayOrder))
                .toList();

        List<FinancialReportTreeNodeDto> childNodes = children.stream()
                .map(c -> buildNode(c, all, computedByCode))
                .toList();

        BigDecimal value = computedByCode.getOrDefault(section.getCode(), BigDecimal.ZERO);

        return new FinancialReportTreeNodeDto(
                section.getId(),
                section.getCode(),
                section.getTitle(),
                section.getSectionType(),
                value,
                childNodes
        );
    }
}

