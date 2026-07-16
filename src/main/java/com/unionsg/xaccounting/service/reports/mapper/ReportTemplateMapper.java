package com.unionsg.xaccounting.service.reports.mapper;

import com.unionsg.xaccounting.dto.reports.ReportTemplateRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateResponseDto;
import com.unionsg.xaccounting.entity.reports.ReportTemplate;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ReportTemplateMapper {

    public ReportTemplateResponseDto toResponse(ReportTemplate entity) {
        if (entity == null) return null;

        return new ReportTemplateResponseDto(
                entity.getId(),
                entity.getTemplateCode(),
                entity.getTemplateName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getStatus(),
                entity.getVersion(),
                entity.isSystemTemplate(),
                entity.getCreatedBy(),
                entity.getCreatedDate(),
                entity.getUpdatedBy(),
                entity.getUpdatedDate()
        );
    }

    public ReportTemplate toEntityForCreate(ReportTemplateRequestDto dto, String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        return ReportTemplate.builder()
                .templateCode(dto.templateCode())
                .templateName(dto.templateName())
                .description(dto.description())
                .category(dto.category())
                .status(dto.status())
                .version(dto.version())
                .isSystemTemplate(dto.isSystemTemplate())
                .createdBy(createdBy)
                .createdDate(now)
                .updatedBy(null)
                .updatedDate(null)
                .build();
    }


    public void applyUpdates(ReportTemplate entity, ReportTemplateRequestDto dto, String updatedBy) {
        entity.setTemplateCode(dto.templateCode());
        entity.setTemplateName(dto.templateName());
        entity.setDescription(dto.description());
        entity.setCategory(dto.category());
        entity.setStatus(dto.status());
        entity.setVersion(dto.version());
        entity.setSystemTemplate(dto.isSystemTemplate());
        entity.setUpdatedBy(updatedBy);
        entity.setUpdatedDate(LocalDateTime.now());
    }
}

