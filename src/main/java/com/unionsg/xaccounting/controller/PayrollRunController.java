package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollRunRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollRunResponse;
import com.unionsg.xaccounting.dto.payroll.ReversePayrollRunRequest;
import com.unionsg.xaccounting.service.payroll.PayrollRunService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * The Payroll Run lifecycle endpoints (§20-§30). Each action here is a separate authorization
 * event by design - see {@code PayrollRunService}'s class doc for the full state machine.
 */
@RestController
@RequestMapping("/api/payroll/runs")
@RequiredArgsConstructor
public class PayrollRunController {

    private final PayrollRunService payrollRunService;

    @PostMapping
    public ResponseEntity<PayrollRunResponse> create(@Valid @RequestBody CreatePayrollRunRequest request) {
        return ResponseEntity.ok(payrollRunService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PayrollRunResponse>> getAll() {
        return ResponseEntity.ok(payrollRunService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayrollRunResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(payrollRunService.getById(id));
    }

    @PostMapping("/{id}/calculate")
    public ResponseEntity<PayrollRunResponse> calculate(@PathVariable Long id) {
        return ResponseEntity.ok(payrollRunService.calculate(id));
    }

    @PostMapping("/{id}/submit-for-review")
    public ResponseEntity<PayrollRunResponse> submitForReview(@PathVariable Long id) {
        return ResponseEntity.ok(payrollRunService.submitForReview(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PayrollRunResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(payrollRunService.approve(id));
    }

    @PostMapping("/{id}/post")
    public ResponseEntity<PayrollRunResponse> post(@PathVariable Long id) {
        return ResponseEntity.ok(payrollRunService.post(id));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<PayrollRunResponse> pay(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate) {
        return ResponseEntity.ok(payrollRunService.pay(id, paymentDate));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<PayrollRunResponse> reverse(@PathVariable Long id, @Valid @RequestBody ReversePayrollRunRequest request) {
        return ResponseEntity.ok(payrollRunService.reverse(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PayrollRunResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(payrollRunService.cancel(id));
    }
}
