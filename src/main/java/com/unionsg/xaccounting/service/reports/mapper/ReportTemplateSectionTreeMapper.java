package com.unionsg.xaccounting.service.reports.mapper;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionTreeDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportTemplateSectionTreeMapper {

    public ReportTemplateSectionTreeDto toTreeNode(ReportTemplateSection section, List<ReportTemplateSection> allSections) {
        if (section == null) return null;

        List<ReportTemplateSection> children = allSections.stream()
                .filter(s -> s.getParentSection() != null && s.getParentSection().getId() != null)
                .filter(s -> s.getParentSection().getId().equals(section.getId()))
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .toList();

        boolean collapsed = !section.isExpandedByDefault();

        return new ReportTemplateSectionTreeDto(
                section.getId(),
                section.getReportTemplate() != null ? section.getReportTemplate().getId() : null,
                section.getParentSection() != null ? section.getParentSection().getId() : null,
                section.getSectionCode(),
                section.getTitle(),
                section.getDisplayOrder(),
                section.getSectionType(),
                section.getFormula(),
                section.isVisible(),
                section.isExpandedByDefault(),
                collapsed,
                children.stream().map(child -> toTreeNode(child, allSections)).toList()
        );
    }
}

