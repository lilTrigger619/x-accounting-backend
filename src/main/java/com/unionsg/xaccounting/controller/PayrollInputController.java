package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollInputRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollInputResponse;
import com.unionsg.xaccounting.service.payroll.PayrollInputService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll/inputs")
@RequiredArgsConstructor
public class PayrollInputController {

    private final PayrollInputService payrollInputService;

    @PostMapping
    public ResponseEntity<PayrollInputResponse> create(@Valid @RequestBody CreatePayrollInputRequest request) {
        return ResponseEntity.ok(payrollInputService.create(request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PayrollInputResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(payrollInputService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PayrollInputResponse> reject(@PathVariable Long id) {
        return ResponseEntity.ok(payrollInputService.reject(id));
    }
}
