package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.downpayment.AllocateDownpaymentRequest;
import com.unionsg.xaccounting.dto.downpayment.CounterpartyDownpaymentSummary;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentAgingRow;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentAllocationResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentBalanceRow;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentDashboardResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentMovementRow;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentRequest;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentStatementResponse;
import com.unionsg.xaccounting.dto.downpayment.OpenDocumentResponse;
import com.unionsg.xaccounting.dto.downpayment.RefundDownpaymentRequest;
import com.unionsg.xaccounting.dto.downpayment.ReverseRequest;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import com.unionsg.xaccounting.service.downpayment.DownpaymentReportService;
import com.unionsg.xaccounting.service.downpayment.DownpaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/downpayments")
@RequiredArgsConstructor
public class DownpaymentController {

    private final DownpaymentService service;
    private final DownpaymentReportService reports;

    // ---- Enquiry ----

    @GetMapping
    public ResponseEntity<Page<DownpaymentResponse>> list(
            @RequestParam(required = false) DownpaymentType type,
            @RequestParam(required = false) DownpaymentStatus status,
            @RequestParam(required = false) Long counterpartyId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Boolean availableOnly,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(type, status, counterpartyId, search, from, to, availableOnly, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DownpaymentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/available")
    public ResponseEntity<List<DownpaymentResponse>> available(
            @RequestParam DownpaymentType type,
            @RequestParam Long counterpartyId,
            @RequestParam(required = false) String currency
    ) {
        return ResponseEntity.ok(service.available(type, counterpartyId, currency));
    }

    @GetMapping("/{id}/open-documents")
    public ResponseEntity<List<OpenDocumentResponse>> openDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(service.openDocuments(id));
    }

    @GetMapping("/by-invoice/{invoiceId}")
    public ResponseEntity<List<DownpaymentAllocationResponse>> byInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(service.allocationsForInvoice(invoiceId));
    }

    @GetMapping("/by-bill/{billId}")
    public ResponseEntity<List<DownpaymentAllocationResponse>> byBill(@PathVariable Long billId) {
        return ResponseEntity.ok(service.allocationsForBill(billId));
    }

    @GetMapping("/counterparty-summary")
    public ResponseEntity<CounterpartyDownpaymentSummary> counterpartySummary(
            @RequestParam DownpaymentType type, @RequestParam Long counterpartyId) {
        return ResponseEntity.ok(reports.counterpartySummary(type, counterpartyId));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DownpaymentDashboardResponse> dashboard() {
        return ResponseEntity.ok(reports.dashboard());
    }

    // ---- Origination / update / delete ----

    @PostMapping
    public ResponseEntity<DownpaymentResponse> create(@RequestBody DownpaymentRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DownpaymentResponse> update(@PathVariable Long id, @RequestBody DownpaymentRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Lifecycle ----

    @PostMapping("/{id}/post")
    public ResponseEntity<DownpaymentResponse> post(@PathVariable Long id) {
        return ResponseEntity.ok(service.post(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<DownpaymentResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<DownpaymentResponse> reverse(@PathVariable Long id, @RequestBody(required = false) ReverseRequest request) {
        return ResponseEntity.ok(service.reverse(id, request != null ? request.getReason() : null));
    }

    @PostMapping("/{id}/allocations")
    public ResponseEntity<DownpaymentResponse> allocate(@PathVariable Long id, @RequestBody AllocateDownpaymentRequest request) {
        return ResponseEntity.ok(service.allocate(id, request));
    }

    @PostMapping("/{id}/allocations/{allocationId}/reverse")
    public ResponseEntity<DownpaymentResponse> reverseAllocation(
            @PathVariable Long id, @PathVariable Long allocationId, @RequestBody(required = false) ReverseRequest request) {
        return ResponseEntity.ok(service.reverseAllocation(id, allocationId, request != null ? request.getReason() : null));
    }

    @PostMapping("/{id}/refunds")
    public ResponseEntity<DownpaymentResponse> refund(@PathVariable Long id, @RequestBody RefundDownpaymentRequest request) {
        return ResponseEntity.ok(service.refund(id, request));
    }

    @PostMapping("/{id}/refunds/{refundId}/reverse")
    public ResponseEntity<DownpaymentResponse> reverseRefund(
            @PathVariable Long id, @PathVariable Long refundId, @RequestBody(required = false) ReverseRequest request) {
        return ResponseEntity.ok(service.reverseRefund(id, refundId, request != null ? request.getReason() : null));
    }

    // ---- Statement and reports ----

    @GetMapping("/statement")
    public ResponseEntity<DownpaymentStatementResponse> statement(
            @RequestParam(required = false) DownpaymentType type,
            @RequestParam(required = false) Long counterpartyId,
            @RequestParam(required = false) Long downpaymentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(reports.statement(type, counterpartyId, downpaymentId, from, to));
    }

    /** Customer downpayment liability (type=CUSTOMER_DOWNPAYMENT) or supplier downpayment asset report. */
    @GetMapping("/reports/balances")
    public ResponseEntity<List<DownpaymentBalanceRow>> balances(
            @RequestParam DownpaymentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(reports.balances(type, asOf));
    }

    @GetMapping("/reports/unallocated")
    public ResponseEntity<List<DownpaymentResponse>> unallocated(@RequestParam(required = false) DownpaymentType type) {
        return ResponseEntity.ok(reports.unallocated(type));
    }

    @GetMapping("/reports/aging")
    public ResponseEntity<List<DownpaymentAgingRow>> aging(
            @RequestParam DownpaymentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(reports.aging(type, asOf));
    }

    @GetMapping("/reports/allocations")
    public ResponseEntity<List<DownpaymentAllocationResponse>> allocations(
            @RequestParam(required = false) DownpaymentType type,
            @RequestParam(required = false) Long counterpartyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeReversed) {
        return ResponseEntity.ok(reports.allocations(type, counterpartyId, from, to, includeReversed));
    }

    @GetMapping("/reports/movement")
    public ResponseEntity<List<DownpaymentMovementRow>> movement(
            @RequestParam DownpaymentType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reports.movement(type, from, to));
    }
}
