package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.loan.CreateLoanTypeRequest;
import com.unionsg.xaccounting.dto.loan.LoanTypeResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.loan.LoanTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-types")
@RequiredArgsConstructor
public class LoanTypeController {

    private final LoanTypeService service;

    @PostMapping
    @RequirePermission(value = "manage_loan_types", group = "Settings")
    public ResponseEntity<LoanTypeResponse> create(@RequestBody CreateLoanTypeRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<LoanTypeResponse>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(activeOnly ? service.listActive() : service.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanTypeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_loan_types", group = "Settings")
    public ResponseEntity<LoanTypeResponse> update(@PathVariable Long id, @RequestBody CreateLoanTypeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/active")
    @RequirePermission(value = "manage_loan_types", group = "Settings")
    public ResponseEntity<LoanTypeResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(service.setActive(id, active));
    }

    @PostMapping("/{id}/deactivate")
    @RequirePermission(value = "manage_loan_types", group = "Settings")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_loan_types", group = "Settings")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
