package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionAccountRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionAccountResponseDto;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateSectionAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/report-template-sections")
@RequiredArgsConstructor
public class ReportTemplateSectionAccountController {

    private final ReportTemplateSectionAccountService service;

    @PostMapping("/{sectionId}/accounts")
    public ResponseEntity<ReportTemplateSectionAccountResponseDto> assign(@PathVariable Long sectionId,
                                                                            @Valid @RequestBody ReportTemplateSectionAccountRequestDto request) {
        return ResponseEntity.ok(service.assign(sectionId, request));
    }

    @GetMapping("/{sectionId}/accounts")
    public ResponseEntity<List<ReportTemplateSectionAccountResponseDto>> listBySectionId(@PathVariable Long sectionId) {
        return ResponseEntity.ok(service.listBySectionId(sectionId));
    }

    @DeleteMapping("/{sectionId}/accounts/{accountId}")
    public ResponseEntity<Void> remove(@PathVariable Long sectionId, @PathVariable Long accountId) {
        service.remove(sectionId, accountId);
        return ResponseEntity.noContent().build();
    }
}

