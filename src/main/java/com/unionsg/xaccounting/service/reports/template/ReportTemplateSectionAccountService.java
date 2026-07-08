package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionAccountRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionAccountResponseDto;

import java.util.List;

public interface ReportTemplateSectionAccountService {

    ReportTemplateSectionAccountResponseDto assign(Long sectionId, ReportTemplateSectionAccountRequestDto request);

    void remove(Long sectionId, Long accountId);

    List<ReportTemplateSectionAccountResponseDto> listBySectionId(Long sectionId);
}

