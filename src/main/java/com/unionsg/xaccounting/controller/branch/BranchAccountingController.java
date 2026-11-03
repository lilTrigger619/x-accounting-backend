package com.unionsg.xaccounting.controller.branch;

import com.unionsg.xaccounting.dto.branch.BranchAccountingSettingsRequest;
import com.unionsg.xaccounting.dto.branch.BranchAccountingSettingsResponse;
import com.unionsg.xaccounting.dto.branch.CostCenterPnlResponse;
import com.unionsg.xaccounting.dto.branch.InterBranchReconciliationResponse;
import com.unionsg.xaccounting.dto.branch.MyScopeResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.branch.BranchAccountingSettingsService;
import com.unionsg.xaccounting.service.branch.BranchReportService;
import com.unionsg.xaccounting.service.branch.UserAccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/branch-accounting")
@RequiredArgsConstructor
public class BranchAccountingController {

    private final BranchAccountingSettingsService settingsService;
    private final UserAccessScopeService userAccessScopeService;
    private final BranchReportService reportService;

    @GetMapping("/settings")
    public ResponseEntity<BranchAccountingSettingsResponse> settings() {
        return ResponseEntity.ok(settingsService.get());
    }

    @PutMapping("/settings")
    @RequirePermission(value = "manage_branch_settings", group = "Settings")
    public ResponseEntity<BranchAccountingSettingsResponse> updateSettings(@RequestBody BranchAccountingSettingsRequest request) {
        return ResponseEntity.ok(settingsService.update(request));
    }

    /** Branches and cost centers the caller may pick, plus their default. */
    @GetMapping("/my-scope")
    public ResponseEntity<MyScopeResponse> myScope() {
        return ResponseEntity.ok(userAccessScopeService.myScope());
    }

    @GetMapping("/inter-branch-reconciliation")
    public ResponseEntity<InterBranchReconciliationResponse> interBranchReconciliation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        return ResponseEntity.ok(reportService.interBranchReconciliation(asOf != null ? asOf : LocalDate.now()));
    }

    @GetMapping("/cost-center-pnl")
    public ResponseEntity<CostCenterPnlResponse> costCenterPnl(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) List<Long> branchIds,
            @RequestParam(required = false) List<Long> costCenterIds
    ) {
        return ResponseEntity.ok(reportService.costCenterPnl(from, to, branchIds, costCenterIds));
    }
}
