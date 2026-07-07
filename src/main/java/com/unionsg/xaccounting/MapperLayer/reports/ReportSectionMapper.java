package com.unionsg.xaccounting.MapperLayer.reports;

import com.unionsg.xaccounting.dto.reports.ReportSectionResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportSection;
import org.springframework.stereotype.Component;

@Component
public class ReportSectionMapper {

    public ReportSectionResponseDto toResponse(ReportSection entity) {
        if (entity == null) {
            return null;
        }

        return new ReportSectionResponseDto(
                entity.getId(),
                entity.getReportDefinition() != null ? entity.getReportDefinition().getId() : null,
                entity.getParentSection() != null ? entity.getParentSection().getId() : null,
                entity.getTitle(),
                entity.getCode(),
                entity.getDisplayOrder(),
                entity.getSectionType(),
                entity.getFormula(),
                entity.isActive()
        );
    }
}

