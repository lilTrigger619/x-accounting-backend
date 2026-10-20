package com.unionsg.xaccounting.controller.fixedasset;

import com.unionsg.xaccounting.MapperLayer.FixedAssetMapper;
import com.unionsg.xaccounting.dto.fixedasset.CreateDepreciationRunRequest;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunActionRequest;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunListItemResponse;
import com.unionsg.xaccounting.dto.fixedasset.DepreciationRunResponse;
import com.unionsg.xaccounting.dto.fixedasset.FixedAssetAuditLogResponse;
import com.unionsg.xaccounting.dto.fixedasset.PeriodDepreciationStatusResponse;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import com.unionsg.xaccounting.repository.fixedasset.FixedAssetAuditLogRepository;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.fixedasset.DepreciationRunService;
import com.unionsg.xaccounting.service.fixedasset.DepreciationRunService.RunSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/depreciation-runs")
@RequiredArgsConstructor
public class DepreciationRunController {

    private static final String GROUP = "Fixed Assets";

    private final DepreciationRunService service;
    private final FixedAssetAuditLogRepository auditLogRepository;

    @PostMapping
    @RequirePermission(value = "calculate_depreciation", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> calculate(@RequestBody CreateDepreciationRunRequest request) {
        return ResponseEntity.ok(service.calculate(request));
    }

    @GetMapping
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<Page<DepreciationRunListItemResponse>> list(
            @RequestParam(required = false) DepreciationRunStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(new RunSearchCriteria(status, from, to), pageable));
    }

    @GetMapping("/periods")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<List<PeriodDepreciationStatusResponse>> periods() {
        return ResponseEntity.ok(service.periods());
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/{id}/activity")
    @RequirePermission(value = "view_fixed_assets", group = GROUP)
    public ResponseEntity<List<FixedAssetAuditLogResponse>> activity(@PathVariable Long id) {
        service.getEntity(id);
        return ResponseEntity.ok(auditLogRepository.findByRunIdOrderByPerformedAtDescIdDesc(id).stream()
                .map(FixedAssetMapper::toAudit).toList());
    }

    @PostMapping("/{id}/recalculate")
    @RequirePermission(value = "calculate_depreciation", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> recalculate(@PathVariable Long id) {
        return ResponseEntity.ok(service.recalculate(id));
    }

    @PostMapping("/{id}/submit")
    @RequirePermission(value = "calculate_depreciation", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(service.submit(id));
    }

    @PostMapping("/{id}/approve")
    @RequirePermission(value = "approve_depreciation", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/{id}/reject")
    @RequirePermission(value = "approve_depreciation", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> reject(@PathVariable Long id, @RequestBody DepreciationRunActionRequest request) {
        return ResponseEntity.ok(service.reject(id, request));
    }

    @PostMapping("/{id}/post")
    @RequirePermission(value = "post_depreciation", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> post(@PathVariable Long id) {
        return ResponseEntity.ok(service.post(id));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(value = "calculate_depreciation", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> cancel(@PathVariable Long id, @RequestBody DepreciationRunActionRequest request) {
        return ResponseEntity.ok(service.cancel(id, request));
    }

    @PostMapping("/{id}/reverse")
    @RequirePermission(value = "post_depreciation", group = GROUP)
    public ResponseEntity<DepreciationRunResponse> reverse(@PathVariable Long id, @RequestBody DepreciationRunActionRequest request) {
        return ResponseEntity.ok(service.reverse(id, request));
    }
}
