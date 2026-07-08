package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.dto.reports.*;
import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;

import java.util.List;

public interface ReportTemplateDesignerAccountAssignmentService {

    ReportTemplateSectionDesignerResponseDto assignAccounts(Long sectionId, DesignerSectionAccountAssignRequestDto request);

    ReportTemplateSectionDesignerResponseDto removeAccounts(Long sectionId, DesignerSectionAccountRemoveRequestDto request);

    DesignerSectionAccountListResponseDto listAssignedAccounts(Long sectionId);

    DesignerAccountSearchPageResponseDto searchUnassignedAccounts(Long sectionId,
                                                                      String search,
                                                                      AccountType accountType,
                                                                      AccountStatus status,
                                                                      int page,
                                                                      int size);

    DesignerAccountSearchPageResponseDto searchAssignedAccounts(Long sectionId,
                                                                      String search,
                                                                      AccountType accountType,
                                                                      AccountStatus status,
                                                                      int page,
                                                                      int size);
}

