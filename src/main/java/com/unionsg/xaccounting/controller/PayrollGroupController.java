package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollGroupRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollGroupResponse;
import com.unionsg.xaccounting.service.payroll.PayrollGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/groups")
@RequiredArgsConstructor
public class PayrollGroupController {

    private final PayrollGroupService payrollGroupService;

    @PostMapping
    public ResponseEntity<PayrollGroupResponse> create(@Valid @RequestBody CreatePayrollGroupRequest request) {
        return ResponseEntity.ok(payrollGroupService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PayrollGroupResponse> update(@PathVariable Long id, @Valid @RequestBody CreatePayrollGroupRequest request) {
        return ResponseEntity.ok(payrollGroupService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<PayrollGroupResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(payrollGroupService.setActive(id, active));
    }

    @GetMapping
    public ResponseEntity<List<PayrollGroupResponse>> getAll() {
        return ResponseEntity.ok(payrollGroupService.getAll());
    }
}
