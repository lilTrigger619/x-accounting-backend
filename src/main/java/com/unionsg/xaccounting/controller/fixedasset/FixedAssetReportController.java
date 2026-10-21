package com.unionsg.xaccounting.controller.fixedasset;

import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.AccumulatedDepreciationReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.AcquisitionReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.BalancesReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.DashboardResponse;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.DepreciationByPeriodReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.DisposalReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.JournalHistoryReport;
import com.unionsg.xaccounting.dto.fixedasset.report.FixedAssetReports.ReconciliationReport;
import com.unionsg.xaccounting.dto.fixedasset.ScheduleLineResponse;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetReportService;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetReportService.ReportFilter;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fixed-assets/reports")
@RequiredArgsConstructor
public class FixedAssetReportController {

    private static final String GROUP = "Fixed Assets";

    private final FixedAssetReportService service;
    private final FixedAssetService assetService;

    @GetMapping("/dashboard")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @GetMapping("/register")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<BalancesReport> register(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) FixedAssetStatus status
    ) {
        return ResponseEntity.ok(service.balances(asOfDate, new ReportFilter(categoryId, locationId, departmentId, status)));
    }

    /** Same balances as the register; the screen presents them grouped by category. */
    @GetMapping("/net-book-value")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<BalancesReport> netBookValue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long departmentId
    ) {
        return ResponseEntity.ok(service.balances(asOfDate, new ReportFilter(categoryId, locationId, departmentId, null)));
    }

    @GetMapping("/schedule/{assetId}")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<List<ScheduleLineResponse>> schedule(
            @PathVariable Long assetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        List<ScheduleLineResponse> rows = assetService.schedule(assetId).stream()
                .filter(r -> fromDate == null || r.getToDate() == null || !r.getToDate().isBefore(fromDate))
                .filter(r -> toDate == null || r.getFromDate() == null || !r.getFromDate().isAfter(toDate))
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/depreciation-by-period")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<DepreciationByPeriodReport> depreciationByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long departmentId
    ) {
        return ResponseEntity.ok(service.depreciationByPeriod(fromDate, toDate, new ReportFilter(categoryId, locationId, departmentId, null)));
    }

    @GetMapping("/accumulated-depreciation")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<AccumulatedDepreciationReport> accumulatedDepreciation(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long departmentId
    ) {
        return ResponseEntity.ok(service.accumulatedDepreciation(fromDate, toDate, new ReportFilter(categoryId, locationId, departmentId, null)));
    }

    @GetMapping("/acquisitions")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<AcquisitionReport> acquisitions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long departmentId
    ) {
        return ResponseEntity.ok(service.acquisitions(fromDate, toDate, new ReportFilter(categoryId, locationId, departmentId, null)));
    }

    @GetMapping("/disposals")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<DisposalReport> disposals(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long departmentId
    ) {
        return ResponseEntity.ok(service.disposals(fromDate, toDate, new ReportFilter(categoryId, locationId, departmentId, null)));
    }

    @GetMapping("/reconciliation")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<ReconciliationReport> reconciliation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate
    ) {
        return ResponseEntity.ok(service.reconciliation(fromDate, asOfDate));
    }

    @GetMapping("/journal-history")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<JournalHistoryReport> journalHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long assetId
    ) {
        return ResponseEntity.ok(service.journalHistory(fromDate, toDate, assetId));
    }
}
