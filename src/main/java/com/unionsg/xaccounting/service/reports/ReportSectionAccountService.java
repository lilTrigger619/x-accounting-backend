package com.unionsg.xaccounting.service.reports;

import com.unionsg.xaccounting.dto.reports.ReportSectionAccountBulkRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportSectionAccountListResponseDto;
import com.unionsg.xaccounting.dto.reports.ReportSectionAccountRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportSectionAccountResponseDto;

import java.util.List;

public interface ReportSectionAccountService {

    ReportSectionAccountResponseDto assignAccount(ReportSectionAccountRequestDto request);

    void removeAssignment(Long reportSectionId, Long accountId);

    ReportSectionAccountListResponseDto listAssignments(Long reportSectionId);

    List<ReportSectionAccountResponseDto> bulkAssign(ReportSectionAccountBulkRequestDto request);
}

