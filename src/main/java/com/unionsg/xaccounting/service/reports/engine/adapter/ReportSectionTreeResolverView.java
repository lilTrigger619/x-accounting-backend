package com.unionsg.xaccounting.service.reports.engine.adapter;

import com.unionsg.xaccounting.dto.reports.FinancialReportTreeNodeDto;
import com.unionsg.xaccounting.enums.SectionType;
import com.unionsg.xaccounting.service.reports.engine.view.ReportSectionView;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Builds the report tree for view-only section graphs.
 */
import org.springframework.stereotype.Component;

@Component
public class ReportSectionTreeResolverView {


    public FinancialReportTreeNodeDto buildTree(List<ReportSectionView> sections,
                                                 Map<String, BigDecimal> computedByCode) {
        if (sections == null || sections.isEmpty()) {
            return new FinancialReportTreeNodeDto(null, "ROOT", "ROOT", SectionType.SECTION, BigDecimal.ZERO, List.of());
        }

        List<ReportSectionView> roots = sections.stream()
                .filter(s -> s.parentSectionCode() == null)
                .sorted(Comparator.comparingInt(ReportSectionView::displayOrder))
                .toList();

        List<FinancialReportTreeNodeDto> rootChildren = roots.stream()
                .map(s -> buildNode(s, sections, computedByCode))
                .toList();

        return new FinancialReportTreeNodeDto(null, "ROOT", "ROOT", SectionType.SECTION, BigDecimal.ZERO, rootChildren);
    }

    private FinancialReportTreeNodeDto buildNode(ReportSectionView section,
                                                 List<ReportSectionView> all,
                                                 Map<String, BigDecimal> computedByCode) {

        List<ReportSectionView> children = all.stream()
                .filter(s -> section.code().equals(s.parentSectionCode()))
                .sorted(Comparator.comparingInt(ReportSectionView::displayOrder))
                .toList();

        List<FinancialReportTreeNodeDto> childNodes = children.stream()
                .map(c -> buildNode(c, all, computedByCode))
                .toList();

        BigDecimal value = computedByCode.getOrDefault(section.code(), BigDecimal.ZERO);

        return new FinancialReportTreeNodeDto(
                section.id(),
                section.code(),
                section.title(),
                section.sectionType(),
                value,
                childNodes
        );
    }
}

