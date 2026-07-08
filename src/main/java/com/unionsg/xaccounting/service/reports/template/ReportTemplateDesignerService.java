package com.unionsg.xaccounting.service.reports.template;

import com.unionsg.xaccounting.dto.reports.*;

public interface ReportTemplateDesignerService {

    ReportTemplateSectionDesignerResponseDto createSection(Long templateId, ReportTemplateSectionCreateRequestDto request);

    ReportTemplateSectionDesignerResponseDto renameSection(ReportTemplateSectionRenameRequestDto request);

    ReportTemplateSectionDesignerResponseDto deleteSection(ReportTemplateSectionDeleteRequestDto request);

    ReportTemplateSectionDesignerResponseDto moveSection(ReportTemplateSectionMoveRequestDto request);

    ReportTemplateSectionDesignerResponseDto changeDisplayOrder(ReportTemplateSectionDisplayOrderRequestDto request);

    ReportTemplateSectionDesignerResponseDto changeParent(ReportTemplateSectionMoveRequestDto request);

    ReportTemplateSectionDesignerResponseDto collapseExpand(ReportTemplateSectionCollapseExpandRequestDto request);

    ReportTemplateSectionDesignerResponseDto duplicateSection(ReportTemplateSectionDuplicateRequestDto request);

    ReportTemplateSectionDesignerResponseDto getTree(Long templateId);
}

