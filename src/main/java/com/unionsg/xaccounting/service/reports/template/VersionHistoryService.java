package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.dto.reports.ReportVersionDetailDto;
import com.unionsg.xaccounting.dto.reports.ReportVersionDto;
import com.unionsg.xaccounting.dto.reports.RollbackResponseDto;

import java.util.List;

public interface VersionHistoryService {

    List<ReportVersionDto> getVersions(Long templateId);

    ReportVersionDetailDto getVersion(Long versionId);

    RollbackResponseDto rollback(Long versionId, String updatedBy);
}

