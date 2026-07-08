package com.unionsg.xaccounting.service.reports.mapper;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSection;
import com.unionsg.xaccounting.entity.reports.ReportTemplateSectionAccount;
import org.springframework.stereotype.Component;

@Component
public class ReportTemplateSectionMapper {

    public ReportTemplateSectionResponseDto toResponse(ReportTemplateSection entity) {
        if (entity == null) return null;

        return new ReportTemplateSectionResponseDto(
                entity.getId(),
                entity.getReportTemplate() != null ? entity.getReportTemplate().getId() : null,
                entity.getParentSection() != null ? entity.getParentSection().getId() : null,
                entity.getSectionCode(),
                entity.getTitle(),
                entity.getDisplayOrder(),
                entity.getSectionType(),
                entity.getFormula(),
                entity.isVisible(),
                entity.isExpandedByDefault()
        );
    }

    public ReportTemplateSection toEntityForCreate(ReportTemplateSectionRequestDto dto,
                                                     ReportTemplate template,
                                                     ReportTemplateSection parentSection) {
        return ReportTemplateSection.builder()
                .reportTemplate(template)
                .parentSection(parentSection)
                .sectionCode(dto.sectionCode())
                .title(dto.title())
                .displayOrder(dto.displayOrder())
                .sectionType(dto.sectionType())
                .formula(dto.formula())
                .visible(dto.visible())
                .expandedByDefault(dto.expandedByDefault())
                .build();
    }

    public void applyUpdates(ReportTemplateSection entity,
                                ReportTemplateSectionRequestDto dto,
                                ReportTemplateSection parentSection) {
        entity.setParentSection(parentSection);
        entity.setSectionCode(dto.sectionCode());
        entity.setTitle(dto.title());
        entity.setDisplayOrder(dto.displayOrder());
        entity.setSectionType(dto.sectionType());
        entity.setFormula(dto.formula());
        entity.setVisible(dto.visible());
        entity.setExpandedByDefault(dto.expandedByDefault());
    }
}

