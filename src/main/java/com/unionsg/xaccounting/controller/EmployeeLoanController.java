package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreateEmployeeLoanRequest;
import com.unionsg.xaccounting.dto.payroll.EmployeeLoanResponse;
import com.unionsg.xaccounting.service.payroll.EmployeeLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/loans")
@RequiredArgsConstructor
public class EmployeeLoanController {

    private final EmployeeLoanService employeeLoanService;

    @PostMapping
    public ResponseEntity<EmployeeLoanResponse> disburse(@Valid @RequestBody CreateEmployeeLoanRequest request) {
        return ResponseEntity.ok(employeeLoanService.disburse(request));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeLoanResponse>> getAllActive() {
        return ResponseEntity.ok(employeeLoanService.getAllActive());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeLoanResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeLoanService.getByEmployee(employeeId));
    }
}
