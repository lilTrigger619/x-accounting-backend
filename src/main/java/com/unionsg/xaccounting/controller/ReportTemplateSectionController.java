package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateSectionResponseDto;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateSectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/report-templates")
@RequiredArgsConstructor
public class ReportTemplateSectionController {

    private final ReportTemplateSectionService service;

    @PostMapping("/{templateId}/sections")
    public ResponseEntity<ReportTemplateSectionResponseDto> create(@PathVariable Long templateId,
                                                                       @Valid @RequestBody ReportTemplateSectionRequestDto request) {
        return ResponseEntity.ok(service.create(templateId, request));
    }

    @GetMapping("/sections/{sectionId}")
    public ResponseEntity<ReportTemplateSectionResponseDto> getById(@PathVariable Long sectionId) {
        return ResponseEntity.ok(service.getById(sectionId));
    }

    @GetMapping("/{templateId}/sections")
    public ResponseEntity<List<ReportTemplateSectionResponseDto>> listByTemplateId(@PathVariable Long templateId) {
        return ResponseEntity.ok(service.listByTemplateId(templateId));
    }

    @PutMapping("/sections/{sectionId}")
    public ResponseEntity<ReportTemplateSectionResponseDto> update(@PathVariable Long sectionId,
                                                                    @Valid @RequestBody ReportTemplateSectionRequestDto request) {
        return ResponseEntity.ok(service.update(sectionId, request));
    }

    @DeleteMapping("/sections/{sectionId}")
    public ResponseEntity<Void> delete(@PathVariable Long sectionId) {
        service.delete(sectionId);
        return ResponseEntity.noContent().build();
    }
}

