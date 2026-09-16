package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.EmployeePayrollRecordResponse;
import com.unionsg.xaccounting.service.payroll.PayrollRunService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Payslips (§39) - always exactly what the corresponding Payroll Run calculated, never a live recomputation. */
@RestController
@RequestMapping("/api/payroll/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayrollRunService payrollRunService;

    @GetMapping("/run/{runId}/employee/{employeeId}")
    public ResponseEntity<EmployeePayrollRecordResponse> getPayslip(@PathVariable Long runId, @PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollRunService.getPayslip(runId, employeeId));
    }
}
