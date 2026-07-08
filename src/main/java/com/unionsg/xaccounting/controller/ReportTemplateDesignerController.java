package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.*;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateDesignerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/report-templates/{templateId}/designer/sections")
@RequiredArgsConstructor
public class ReportTemplateDesignerController {

    private final ReportTemplateDesignerService designerService;

    @PostMapping
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> create(
            @PathVariable Long templateId,
            @Valid @RequestBody ReportTemplateSectionCreateRequestDto request
    ) {
        return ResponseEntity.ok(designerService.createSection(templateId, request));
    }

    @PutMapping("/rename")
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> rename(
            @Valid @RequestBody ReportTemplateSectionRenameRequestDto request
    ) {
        return ResponseEntity.ok(designerService.renameSection(request));
    }

    @DeleteMapping
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> delete(
            @Valid @RequestBody ReportTemplateSectionDeleteRequestDto request
    ) {
        return ResponseEntity.ok(designerService.deleteSection(request));
    }

    @PutMapping("/move")
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> move(
            @Valid @RequestBody ReportTemplateSectionMoveRequestDto request
    ) {
        return ResponseEntity.ok(designerService.moveSection(request));
    }

    @PutMapping("/display-order")
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> changeDisplayOrder(
            @Valid @RequestBody ReportTemplateSectionDisplayOrderRequestDto request
    ) {
        return ResponseEntity.ok(designerService.changeDisplayOrder(request));
    }

    @PutMapping("/parent")
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> changeParent(
            @Valid @RequestBody ReportTemplateSectionMoveRequestDto request
    ) {
        return ResponseEntity.ok(designerService.changeParent(request));
    }

    @PutMapping("/collapse-expand")
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> collapseExpand(
            @Valid @RequestBody ReportTemplateSectionCollapseExpandRequestDto request
    ) {
        return ResponseEntity.ok(designerService.collapseExpand(request));
    }

    @PostMapping("/duplicate")
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> duplicate(
            @Valid @RequestBody ReportTemplateSectionDuplicateRequestDto request
    ) {
        return ResponseEntity.ok(designerService.duplicateSection(request));
    }

    @GetMapping
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> tree(
            @PathVariable Long templateId
    ) {
        return ResponseEntity.ok(designerService.getTree(templateId));
    }
}

