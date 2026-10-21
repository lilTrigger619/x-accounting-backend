package com.unionsg.xaccounting.controller.fixedasset;

import com.unionsg.xaccounting.dto.fixedasset.AssetActionRequest;
import com.unionsg.xaccounting.dto.fixedasset.AssetEstimateChangeResponse;
import com.unionsg.xaccounting.dto.fixedasset.AssetTransactionResponse;
import com.unionsg.xaccounting.dto.fixedasset.ChangeEstimateRequest;
import com.unionsg.xaccounting.dto.fixedasset.DisposalPreviewResponse;
import com.unionsg.xaccounting.dto.fixedasset.DisposeAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetAuditLogResponse;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetListItemResponse;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetResponse;
import com.unionsg.xaccounting.dto.fixedasset.ImpairAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.SaveFixedAssetRequest;
import com.unionsg.xaccounting.dto.fixedasset.ScheduleLineResponse;
import com.unionsg.xaccounting.enums.fixedasset.FixedAssetStatus;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetService;
import com.unionsg.xaccounting.service.fixedasset.FixedAssetService.AssetSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fixed-assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private static final String GROUP = "Fixed Assets";

    private final FixedAssetService service;

    // ---- Register ----

    @PostMapping
    @RequirePermission(value = "manage_fixed_assets", group = GROUP)
    public ResponseEntity<FixedAssetResponse> create(@RequestBody SaveFixedAssetRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_fixed_assets", group = GROUP)
    public ResponseEntity<FixedAssetResponse> update(@PathVariable Long id, @RequestBody SaveFixedAssetRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_fixed_assets", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<Page<FixedAssetListItemResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) FixedAssetStatus status,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquiredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate acquiredTo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(
                new AssetSearchCriteria(search, categoryId, status, locationId, departmentId, acquiredFrom, acquiredTo), pageable));
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<FixedAssetResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/{id}/schedule")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<List<ScheduleLineResponse>> schedule(@PathVariable Long id) {
        return ResponseEntity.ok(service.schedule(id));
    }

    @GetMapping("/{id}/transactions")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<List<AssetTransactionResponse>> transactions(@PathVariable Long id) {
        return ResponseEntity.ok(service.transactions(id));
    }

    @GetMapping("/{id}/activity")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<List<FixedAssetAuditLogResponse>> activity(@PathVariable Long id) {
        return ResponseEntity.ok(service.activity(id));
    }

    @GetMapping("/{id}/estimate-changes")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<List<AssetEstimateChangeResponse>> estimateChanges(@PathVariable Long id) {
        return ResponseEntity.ok(service.estimateChanges(id));
    }

    // ---- Lifecycle ----

    @PostMapping("/{id}/activate")
    @RequirePermission(value = "manage_fixed_assets", group = GROUP)
    public ResponseEntity<FixedAssetResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PostMapping("/{id}/estimate-changes")
    @RequirePermission(value = "manage_fixed_assets", group = GROUP)
    public ResponseEntity<FixedAssetResponse> changeEstimate(@PathVariable Long id, @RequestBody ChangeEstimateRequest request) {
        return ResponseEntity.ok(service.changeEstimate(id, request));
    }

    @PostMapping("/{id}/impair")
    @RequirePermission(value = "manage_fixed_assets", group = GROUP)
    public ResponseEntity<FixedAssetResponse> impair(@PathVariable Long id, @RequestBody ImpairAssetRequest request) {
        return ResponseEntity.ok(service.impair(id, request));
    }

    @PostMapping("/{id}/disposal-preview")
    @RequirePermission(value = "dispose_fixed_assets", group = GROUP)
    public ResponseEntity<DisposalPreviewResponse> previewDisposal(@PathVariable Long id, @RequestBody DisposeAssetRequest request) {
        return ResponseEntity.ok(service.previewDisposal(id, request));
    }

    @PostMapping("/{id}/dispose")
    @RequirePermission(value = "dispose_fixed_assets", group = GROUP)
    public ResponseEntity<FixedAssetResponse> dispose(@PathVariable Long id, @RequestBody DisposeAssetRequest request) {
        return ResponseEntity.ok(service.dispose(id, request));
    }

    @PostMapping("/{id}/transactions/{transactionId}/reverse")
    @RequirePermission(value = "reverse_fixed_asset_transactions", group = GROUP)
    public ResponseEntity<FixedAssetResponse> reverseTransaction(@PathVariable Long id, @PathVariable Long transactionId,
                                                                 @RequestBody AssetActionRequest request) {
        return ResponseEntity.ok(service.reverseTransaction(id, transactionId, request));
    }
}
