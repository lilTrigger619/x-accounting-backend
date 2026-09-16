package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.EmployeePayrollRecordResponse;
import com.unionsg.xaccounting.dto.payroll.PayrollGlReconciliationResponse;
import com.unionsg.xaccounting.service.payroll.PayrollReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/reports")
@RequiredArgsConstructor
public class PayrollReportController {

    private final PayrollReportService payrollReportService;

    @GetMapping("/register/{runId}")
    public ResponseEntity<List<EmployeePayrollRecordResponse>> getRegister(@PathVariable Long runId) {
        return ResponseEntity.ok(payrollReportService.getRegister(runId));
    }

    @GetMapping("/gl-reconciliation")
    public ResponseEntity<PayrollGlReconciliationResponse> getGlReconciliation() {
        return ResponseEntity.ok(payrollReportService.getGlReconciliation());
    }
}
