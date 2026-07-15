package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.FinancialReportEngineRequestDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportSectionsResponseDto;
import com.unionsg.xaccounting.dto.reports.FinancialReportTreeResponseDto;
import com.unionsg.xaccounting.service.reports.engine.FinancialReportEngine;
import com.unionsg.xaccounting.service.reports.engine.FinancialReportSectionsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reports")
public class ReportsController {

    private final FinancialReportEngine engine;
    private final FinancialReportSectionsMapper mapper;

    private final Clock clock = Clock.systemDefaultZone();

    @GetMapping("/api/reports/trial-balance")
    @Operation(summary = "Trial Balance")
    public ResponseEntity<FinancialReportSectionsResponseDto> trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return handle("TRIAL_BALANCE", "Trial Balance", fromDate, toDate);
    }

// Intentionally not exposing profit-loss endpoint here because ProfitAndLossController already provides it.


    @GetMapping("/api/reports/balance-sheet")
    @Operation(summary = "Balance Sheet")
    public ResponseEntity<FinancialReportSectionsResponseDto> balanceSheet(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        System.out.println("Okya");
        return handle("BALANCE_SHEET", "Balance Sheet", fromDate, toDate);
    }

    @GetMapping("/api/reports/cash-flow")
    @Operation(summary = "Cash Flow")
    public ResponseEntity<FinancialReportSectionsResponseDto> cashFlow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return handle("CASH_FLOW", "Cash Flow", fromDate, toDate);
    }

    private ResponseEntity<FinancialReportSectionsResponseDto> handle(
            String reportCode,
            String reportName,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        validateDates(fromDate, toDate);

        FinancialReportTreeResponseDto tree = engine.generate(
                new FinancialReportEngineRequestDto(reportCode, fromDate, toDate)
        );

        return ResponseEntity.ok(mapper.map(tree, reportName));
    }

    private void validateDates(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate cannot be after toDate");
        }

        LocalDate today = LocalDate.now(clock);
        if (fromDate.isAfter(today) || toDate.isAfter(today)) {
            throw new IllegalArgumentException("fromDate and toDate cannot be in the future");
        }
    }
}

