package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.*;
import com.unionsg.xaccounting.enums.AccountStatus;
import com.unionsg.xaccounting.enums.AccountType;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateDesignerAccountAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/report-templates/{templateId}/designer/sections")
@RequiredArgsConstructor
public class ReportTemplateDesignerAccountAssignmentController {

    private final ReportTemplateDesignerAccountAssignmentService assignmentService;

    @PostMapping("/{sectionId}/accounts")
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> assignAccounts(
            @PathVariable Long templateId,
            @PathVariable Long sectionId,
            @Valid @RequestBody DesignerSectionAccountAssignRequestDto request
    ) {
        return ResponseEntity.ok(assignmentService.assignAccounts(sectionId, request));
    }

    @DeleteMapping("/{sectionId}/accounts")
    public ResponseEntity<ReportTemplateSectionDesignerResponseDto> removeAccounts(
            @PathVariable Long templateId,
            @PathVariable Long sectionId,
            @Valid @RequestBody DesignerSectionAccountRemoveRequestDto request
    ) {
        return ResponseEntity.ok(assignmentService.removeAccounts(sectionId, request));
    }

    @GetMapping("/{sectionId}/accounts/assigned")
    public ResponseEntity<DesignerSectionAccountListResponseDto> listAssigned(
            @PathVariable Long templateId,
            @PathVariable Long sectionId
    ) {
        return ResponseEntity.ok(assignmentService.listAssignedAccounts(sectionId));
    }

    @GetMapping("/{sectionId}/accounts/unassigned")
    public ResponseEntity<DesignerAccountSearchPageResponseDto> searchUnassigned(
            @PathVariable Long templateId,
            @PathVariable Long sectionId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountType accountType,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(assignmentService.searchUnassignedAccounts(sectionId, search, accountType, status, page, size));
    }

    @GetMapping("/{sectionId}/accounts/assigned/search")
    public ResponseEntity<DesignerAccountSearchPageResponseDto> searchAssigned(
            @PathVariable Long templateId,
            @PathVariable Long sectionId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountType accountType,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(assignmentService.searchAssignedAccounts(sectionId, search, accountType, status, page, size));
    }
}

