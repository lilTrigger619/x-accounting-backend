package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.dto.reports.ReportTemplateRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateResponseDto;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;

import java.util.List;

public interface ReportTemplateService {

    ReportTemplateResponseDto create(ReportTemplateRequestDto request);

    ReportTemplateResponseDto getById(Long id);

    ReportTemplateResponseDto getByTemplateCode(String templateCode);

    List<ReportTemplateResponseDto> listAll();

    ReportTemplateResponseDto update(Long id, ReportTemplateRequestDto request);

    void delete(Long id);

    ReportTemplateResponseDto setStatus(Long id, ReportTemplateStatus status);

}

