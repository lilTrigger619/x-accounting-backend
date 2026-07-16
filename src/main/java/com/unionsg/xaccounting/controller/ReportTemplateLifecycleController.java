package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplatePreviewRequestDto;
import com.unionsg.xaccounting.security.auth.UserPrincipal;
import com.unionsg.xaccounting.service.reports.template.lifecycle.ReportTemplateLifecycleService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/report-templates")
@RequiredArgsConstructor
public class ReportTemplateLifecycleController {

    private final ReportTemplateLifecycleService lifecycleService;



    @PostMapping("/{id}/preview")
    public ResponseEntity<FinancialReportTreeResponseDto> preview(
            @PathVariable("id") Long templateId,
            @RequestBody ReportTemplatePreviewRequestDto request
    ) {
        return ResponseEntity.ok(
                lifecycleService.preview(templateId, request.fromDate(), request.toDate())
        );
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, Object>> publish(
            @PathVariable("id") Long templateId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String updatedBy = userPrincipal.getUsername();
        lifecycleService.publish(templateId, updatedBy);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Map<String, Object>> archive(
            @PathVariable("id") Long templateId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String updatedBy = userPrincipal.getUsername();
        lifecycleService.archive(templateId, updatedBy);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ReportTemplateDto> clone(
            @PathVariable("id") Long templateId,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        String updatedBy = userPrincipal.getUsername();
        return ResponseEntity.ok(lifecycleService.clone(templateId, updatedBy));
    }



}


