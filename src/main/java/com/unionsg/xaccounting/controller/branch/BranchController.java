package com.unionsg.xaccounting.controller.branch;

import com.unionsg.xaccounting.dto.branch.BranchRequest;
import com.unionsg.xaccounting.dto.branch.BranchResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.branch.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private static final String GROUP = "Settings";

    private final BranchService service;

    @PostMapping
    @RequirePermission(value = "manage_branches", group = GROUP)
    public ResponseEntity<BranchResponse> create(@RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    /** Branches in the caller's scope. */
    @GetMapping
    public ResponseEntity<List<BranchResponse>> list(
            @RequestParam(defaultValue = "false") boolean activeOnly,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(service.list(activeOnly, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_branches", group = GROUP)
    public ResponseEntity<BranchResponse> update(@PathVariable Long id, @RequestBody BranchRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @RequirePermission(value = "manage_branches", group = GROUP)
    public ResponseEntity<BranchResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_branches", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
