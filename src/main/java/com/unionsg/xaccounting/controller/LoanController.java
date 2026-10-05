package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.loan.CreateLoanRequest;
import com.unionsg.xaccounting.dto.loan.LoanAccrualResponse;
import com.unionsg.xaccounting.dto.loan.LoanActionRequest;
import com.unionsg.xaccounting.dto.loan.LoanAuditLogResponse;
import com.unionsg.xaccounting.dto.loan.LoanDashboardResponse;
import com.unionsg.xaccounting.dto.loan.LoanLineResponse;
import com.unionsg.xaccounting.dto.loan.LoanListItemResponse;
import com.unionsg.xaccounting.dto.loan.LoanRepaymentResponse;
import com.unionsg.xaccounting.dto.loan.LoanResponse;
import com.unionsg.xaccounting.dto.loan.LoanSchedulePreviewResponse;
import com.unionsg.xaccounting.dto.loan.LoanStatementResponse;
import com.unionsg.xaccounting.dto.loan.RecordLoanRepaymentRequest;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.loan.LoanReportService;
import com.unionsg.xaccounting.service.loan.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private static final String GROUP = "Loans";

    private final LoanService service;
    private final LoanReportService reportService;

    // ---- Setup ----

    @PostMapping
    @RequirePermission(value = "manage_loans", group = GROUP)
    public ResponseEntity<LoanResponse> create(@RequestBody CreateLoanRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_loans", group = GROUP)
    public ResponseEntity<LoanResponse> update(@PathVariable Long id, @RequestBody CreateLoanRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_loans", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/schedule-preview")
    @RequirePermission(value = "manage_loans", group = GROUP)
    public ResponseEntity<LoanSchedulePreviewResponse> preview(@RequestBody CreateLoanRequest request) {
        return ResponseEntity.ok(service.preview(request));
    }

    // ---- Enquiry ----

    @GetMapping
    @RequirePermission(value = "view_loans", group = GROUP)
    public ResponseEntity<Page<LoanListItemResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LoanDirection direction,
            @RequestParam(required = false) LoanStatus status,
            @RequestParam(required = false) Long loanTypeId,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startTo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(
                new LoanService.LoanSearchCriteria(search, direction, status, loanTypeId, currency, startFrom, startTo), pageable));
    }

    @GetMapping("/dashboard")
    @RequirePermission(value = "view_loans", group = GROUP)
    public ResponseEntity<LoanDashboardResponse> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportService.dashboard(from, to));
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_loans", group = GROUP)
    public ResponseEntity<LoanResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/{id}/schedule")
    @RequirePermission(value = "view_loans", group = GROUP)
    public ResponseEntity<List<LoanLineResponse>> schedule(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSchedule(id));
    }

    @GetMapping("/{id}/repayments")
    @RequirePermission(value = "view_loans", group = GROUP)
    public ResponseEntity<List<LoanRepaymentResponse>> repayments(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRepayments(id));
    }

    @GetMapping("/{id}/accruals")
    @RequirePermission(value = "view_loans", group = GROUP)
    public ResponseEntity<List<LoanAccrualResponse>> accruals(@PathVariable Long id) {
        return ResponseEntity.ok(service.getAccruals(id));
    }

    @GetMapping("/{id}/activity")
    @RequirePermission(value = "view_loans", group = GROUP)
    public ResponseEntity<List<LoanAuditLogResponse>> activity(@PathVariable Long id) {
        return ResponseEntity.ok(service.getActivity(id));
    }

    @GetMapping("/{id}/statement")
    @RequirePermission(value = "view_loans", group = GROUP)
    public ResponseEntity<LoanStatementResponse> statement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reportService.statement(id, from, to));
    }

    // ---- Lifecycle ----

    @PostMapping("/{id}/approve")
    @RequirePermission(value = "approve_loans", group = GROUP)
    public ResponseEntity<LoanResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/{id}/disburse")
    @RequirePermission(value = "approve_loans", group = GROUP)
    public ResponseEntity<LoanResponse> disburse(@PathVariable Long id, @RequestBody(required = false) LoanActionRequest request) {
        return ResponseEntity.ok(service.disburse(id, request));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(value = "manage_loans", group = GROUP)
    public ResponseEntity<LoanResponse> cancel(@PathVariable Long id, @RequestBody(required = false) LoanActionRequest request) {
        return ResponseEntity.ok(service.cancel(id, request));
    }

    @PostMapping("/{id}/repayments")
    @RequirePermission(value = "record_loan_payments", group = GROUP)
    public ResponseEntity<LoanResponse> recordRepayment(@PathVariable Long id, @RequestBody RecordLoanRepaymentRequest request) {
        return ResponseEntity.ok(service.recordRepayment(id, request));
    }

    @PostMapping("/{id}/repayments/{paymentId}/reverse")
    @RequirePermission(value = "reverse_loans", group = GROUP)
    public ResponseEntity<LoanResponse> reverseRepayment(
            @PathVariable Long id, @PathVariable Long paymentId, @RequestBody LoanActionRequest request) {
        return ResponseEntity.ok(service.reversePayment(id, paymentId, request));
    }

    @PostMapping("/{id}/installments/{installmentNumber}/missed")
    @RequirePermission(value = "record_loan_payments", group = GROUP)
    public ResponseEntity<LoanResponse> markMissed(
            @PathVariable Long id, @PathVariable Integer installmentNumber,
            @RequestBody(required = false) LoanActionRequest request) {
        return ResponseEntity.ok(service.markInstallmentMissed(id, installmentNumber, request));
    }

    @PostMapping("/{id}/accrue-interest")
    @RequirePermission(value = "record_loan_payments", group = GROUP)
    public ResponseEntity<LoanResponse> accrueInterest(@PathVariable Long id, @RequestBody LoanActionRequest request) {
        return ResponseEntity.ok(service.accrueInterest(id, request));
    }

    @PostMapping("/{id}/default")
    @RequirePermission(value = "reverse_loans", group = GROUP)
    public ResponseEntity<LoanResponse> markDefaulted(@PathVariable Long id, @RequestBody LoanActionRequest request) {
        return ResponseEntity.ok(service.markDefaulted(id, request));
    }

    @PostMapping("/{id}/close")
    @RequirePermission(value = "reverse_loans", group = GROUP)
    public ResponseEntity<LoanResponse> close(@PathVariable Long id, @RequestBody(required = false) LoanActionRequest request) {
        return ResponseEntity.ok(service.close(id, request));
    }

    @PostMapping("/{id}/reverse")
    @RequirePermission(value = "reverse_loans", group = GROUP)
    public ResponseEntity<LoanResponse> reverse(@PathVariable Long id, @RequestBody LoanActionRequest request) {
        return ResponseEntity.ok(service.reverseLoan(id, request));
    }
}
