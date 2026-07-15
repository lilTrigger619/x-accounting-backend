package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.FinancialReportEngineRequestDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import com.unionsg.xaccounting.service.reports.engine.FinancialReportEngine;
import com.unionsg.xaccounting.enums.ReportTemplateStatus;
import com.unionsg.xaccounting.repository.reports.ReportTemplateRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class FinancialReportEngineController {

    private final FinancialReportEngine engine;
    private final ReportTemplateRepository reportTemplateRepository;


    // POST /api/v1/reports/{reportCode}/engine?from=yyyy-MM-dd&to=yyyy-MM-dd
    @PostMapping("/{reportCode}/engine")
    public ResponseEntity<FinancialReportTreeResponseDto> run(
            @PathVariable String reportCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // Phase 3: resolve a PUBLISHED ReportTemplate by reportCode (templateCode) and execute template pipeline.
        var template = reportTemplateRepository
                .findTopByTemplateCodeAndStatusOrderByVersionDesc(reportCode, ReportTemplateStatus.PUBLISHED)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Published template not found for code: " + reportCode
                ));

        return ResponseEntity.ok(engine.generateFromTemplate(template, from, to));


    }

}

