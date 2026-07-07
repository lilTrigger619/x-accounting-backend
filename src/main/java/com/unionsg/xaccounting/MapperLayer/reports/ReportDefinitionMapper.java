package com.unionsg.xaccounting.MapperLayer.reports;

import com.unionsg.xaccounting.dto.reports.ReportDefinitionResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportDefinition;
import org.springframework.stereotype.Component;

@Component
public class ReportDefinitionMapper {

    public ReportDefinitionResponseDto toResponse(ReportDefinition entity) {
        if (entity == null) {
            return null;
        }

        return new ReportDefinitionResponseDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

