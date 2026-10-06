package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentTypeRequest;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentTypeResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.prepayment.PrepaymentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prepayment-types")
@RequiredArgsConstructor
public class PrepaymentTypeController {

    private final PrepaymentTypeService service;

    @PostMapping
    @RequirePermission(value = "manage_prepayment_types", group = "Settings")
    public ResponseEntity<PrepaymentTypeResponse> create(@RequestBody CreatePrepaymentTypeRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PrepaymentTypeResponse>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(activeOnly ? service.listActive() : service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrepaymentTypeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_prepayment_types", group = "Settings")
    public ResponseEntity<PrepaymentTypeResponse> update(@PathVariable Long id, @RequestBody CreatePrepaymentTypeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @RequirePermission(value = "manage_prepayment_types", group = "Settings")
    public ResponseEntity<PrepaymentTypeResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @PostMapping("/{id}/deactivate")
    @RequirePermission(value = "manage_prepayment_types", group = "Settings")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_prepayment_types", group = "Settings")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
