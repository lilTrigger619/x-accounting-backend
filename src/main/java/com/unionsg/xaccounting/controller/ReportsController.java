package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.cashflow.CashFlowStatementResponse;
import com.unionsg.xaccounting.service.reports.CashFlowStatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final CashFlowStatementService cashFlowStatementService;

    private final Clock clock = Clock.systemDefaultZone();

// Intentionally not exposing profit-loss endpoint here because ProfitAndLossController already provides it.
// Trial Balance and Balance Sheet are likewise not exposed here: BalanceSheetController and
// TrialBalanceController provide the real, working implementations (true as-of-date cumulative
// balances). Routing either through this generic template engine would require a "TRIAL_BALANCE"/
// "BALANCE_SHEET" ReportTemplate that was never seeded, and would apply the engine's period-activity
// semantics - correct for Profit & Loss, but wrong for a balance-sheet-family report, which needs
// every transaction since inception rather than only what falls inside an arbitrary date range.

    /**
     * Indirect-method cash flow statement. It used to go through the template engine, which has no
     * CASH_FLOW template and always failed; it now reads the posted ledger directly like the
     * Balance Sheet and P&L do.
     */
    @GetMapping("/api/reports/cash-flow")
    @Operation(summary = "Cash Flow")
    public ResponseEntity<CashFlowStatementResponse> cashFlow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        validateDates(fromDate, toDate);
        return ResponseEntity.ok(cashFlowStatementService.generate(fromDate, toDate));
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

