package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreatePayrollCalendarPeriodRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollCalendarPeriodResponse;
import com.unionsg.xaccounting.service.payroll.PayrollCalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/calendar-periods")
@RequiredArgsConstructor
public class PayrollCalendarController {

    private final PayrollCalendarService payrollCalendarService;

    @PostMapping
    public ResponseEntity<PayrollCalendarPeriodResponse> create(@Valid @RequestBody CreatePayrollCalendarPeriodRequest request) {
        return ResponseEntity.ok(payrollCalendarService.create(request));
    }

    @GetMapping("/group/{payrollGroupId}")
    public ResponseEntity<List<PayrollCalendarPeriodResponse>> getByGroup(@PathVariable Long payrollGroupId) {
        return ResponseEntity.ok(payrollCalendarService.getByGroup(payrollGroupId));
    }
}
