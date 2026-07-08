package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionResponseDto;

import java.util.List;

public interface ReportTemplateSectionService {

    ReportTemplateSectionResponseDto create(Long templateId, ReportTemplateSectionRequestDto request);

    ReportTemplateSectionResponseDto getById(Long id);

    List<ReportTemplateSectionResponseDto> listByTemplateId(Long templateId);

    ReportTemplateSectionResponseDto update(Long sectionId, ReportTemplateSectionRequestDto request);

    void delete(Long sectionId);
}

