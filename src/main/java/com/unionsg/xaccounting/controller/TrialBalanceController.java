package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.TrialBalanceResponseDTO;
import com.unionsg.xaccounting.service.reports.TrialBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class TrialBalanceController {

    private final com.unionsg.xaccounting.service.branch.ReportScopeResolver reportScopeResolver;

    private final TrialBalanceService trialBalanceService;

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceResponseDTO> getTrialBalance(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOfDate,
            @RequestParam(required = false) java.util.List<Long> branchIds,
            @RequestParam(required = false) java.util.List<Long> costCenterIds
    ) {
        return ResponseEntity.ok(trialBalanceService.generateReport(asOfDate,
                reportScopeResolver.costCenterFilter(branchIds, costCenterIds)));
    }
}
