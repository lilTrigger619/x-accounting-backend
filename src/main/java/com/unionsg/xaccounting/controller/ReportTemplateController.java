package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.ReportTemplateRequestDto;
import com.unionsg.xaccounting.dto.reports.ReportTemplateResponseDto;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.service.reports.template.ReportTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/report-templates")
@RequiredArgsConstructor
public class ReportTemplateController {

    private final ReportTemplateService service;

    @PostMapping
    public ResponseEntity<ReportTemplateResponseDto> create(@Valid @RequestBody ReportTemplateRequestDto request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportTemplateResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<ReportTemplateResponseDto>> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportTemplateResponseDto> update(@PathVariable Long id,
                                                            @Valid @RequestBody ReportTemplateRequestDto request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ReportTemplateResponseDto> setStatus(@PathVariable Long id,
                                                                 @RequestParam ReportTemplateStatus status,
                                                                 @RequestParam(required = false) String updatedBy) {
        return ResponseEntity.ok(service.setStatus(id, status, updatedBy));
    }
}

