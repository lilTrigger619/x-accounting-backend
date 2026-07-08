package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.FinancialReportEngineRequestDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import com.unionsg.xaccounting.service.reports.engine.FinancialReportEngine;
import jakarta.validation.Valid;
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

    // POST /api/v1/reports/{reportCode}/engine?from=yyyy-MM-dd&to=yyyy-MM-dd
    @PostMapping("/{reportCode}/engine")
    public ResponseEntity<FinancialReportTreeResponseDto> run(
            @PathVariable String reportCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        FinancialReportTreeResponseDto response = engine.generate(
                new FinancialReportEngineRequestDto(reportCode, from, to)
        );
        return ResponseEntity.ok(response);
    }
}

