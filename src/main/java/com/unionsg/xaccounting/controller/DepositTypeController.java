package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.deposit.CreateDepositTypeRequest;
import com.unionsg.xaccounting.dto.deposit.DepositTypeResponse;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.deposit.DepositTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deposit-types")
@RequiredArgsConstructor
public class DepositTypeController {

    private final DepositTypeService service;

    @PostMapping
    @RequirePermission(value = "manage_deposit_types", group = "Settings")
    public ResponseEntity<DepositTypeResponse> create(@RequestBody CreateDepositTypeRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<DepositTypeResponse>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(required = false) DepositDirection direction
    ) {
        return ResponseEntity.ok(service.list(activeOnly, direction));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepositTypeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_deposit_types", group = "Settings")
    public ResponseEntity<DepositTypeResponse> update(@PathVariable Long id, @RequestBody CreateDepositTypeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @RequirePermission(value = "manage_deposit_types", group = "Settings")
    public ResponseEntity<DepositTypeResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_deposit_types", group = "Settings")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
