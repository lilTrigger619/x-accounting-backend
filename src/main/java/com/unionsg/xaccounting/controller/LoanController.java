package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.loan.CreateLoanRequest;
import com.unionsg.xaccounting.dto.loan.LoanListItemResponse;
import com.unionsg.xaccounting.dto.loan.LoanRepaymentResponse;
import com.unionsg.xaccounting.dto.loan.LoanResponse;
import com.unionsg.xaccounting.dto.loan.RecordLoanRepaymentRequest;
import com.unionsg.xaccounting.service.loan.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService service;

    @PostMapping
    public ResponseEntity<LoanResponse> create(@RequestBody CreateLoanRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<LoanListItemResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    @GetMapping("/{id}/repayments")
    public ResponseEntity<List<LoanRepaymentResponse>> repayments(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRepayments(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<LoanResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/{id}/disburse")
    public ResponseEntity<LoanResponse> disburse(@PathVariable Long id) {
        return ResponseEntity.ok(service.disburse(id));
    }

    @PostMapping("/{id}/repayments")
    public ResponseEntity<LoanResponse> recordRepayment(
            @PathVariable Long id, @RequestBody RecordLoanRepaymentRequest request
    ) {
        return ResponseEntity.ok(service.recordRepayment(id, request));
    }

    @PostMapping("/{id}/settle")
    public ResponseEntity<LoanResponse> settle(
            @PathVariable Long id, @RequestBody RecordLoanRepaymentRequest request
    ) {
        return ResponseEntity.ok(service.settle(id, request));
    }

    @PostMapping("/{id}/accrue-interest")
    public ResponseEntity<LoanResponse> accrueInterest(
            @PathVariable Long id, @RequestBody Map<String, BigDecimal> body
    ) {
        return ResponseEntity.ok(service.accrueInterest(id, body.get("amount")));
    }

    @PostMapping("/{id}/write-off")
    public ResponseEntity<LoanResponse> writeOff(@PathVariable Long id) {
        return ResponseEntity.ok(service.writeOff(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<LoanResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }
}
