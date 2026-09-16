package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreateSalaryAdvanceRequest;
import com.unionsg.xaccounting.dto.payroll.SalaryAdvanceResponse;
import com.unionsg.xaccounting.service.payroll.SalaryAdvanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/advances")
@RequiredArgsConstructor
public class SalaryAdvanceController {

    private final SalaryAdvanceService salaryAdvanceService;

    @PostMapping
    public ResponseEntity<SalaryAdvanceResponse> issue(@Valid @RequestBody CreateSalaryAdvanceRequest request) {
        return ResponseEntity.ok(salaryAdvanceService.issue(request));
    }

    @GetMapping
    public ResponseEntity<List<SalaryAdvanceResponse>> getAllOutstanding() {
        return ResponseEntity.ok(salaryAdvanceService.getAllOutstanding());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<SalaryAdvanceResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(salaryAdvanceService.getByEmployee(employeeId));
    }
}
