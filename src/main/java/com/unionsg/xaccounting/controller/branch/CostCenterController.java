package com.unionsg.xaccounting.controller.branch;

import com.unionsg.xaccounting.dto.branch.CostCenterRequest;
import com.unionsg.xaccounting.dto.branch.CostCenterResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.branch.CostCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cost-centers")
@RequiredArgsConstructor
public class CostCenterController {

    private static final String GROUP = "Settings";

    private final CostCenterService service;

    @PostMapping
    @RequirePermission(value = "manage_cost_centers", group = GROUP)
    public ResponseEntity<CostCenterResponse> create(@RequestBody CostCenterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    /** Cost centers in the caller's scope, optionally for one branch. */
    @GetMapping
    public ResponseEntity<List<CostCenterResponse>> list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(service.list(branchId, activeOnly, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CostCenterResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_cost_centers", group = GROUP)
    public ResponseEntity<CostCenterResponse> update(@PathVariable Long id, @RequestBody CostCenterRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @RequirePermission(value = "manage_cost_centers", group = GROUP)
    public ResponseEntity<CostCenterResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_cost_centers", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
