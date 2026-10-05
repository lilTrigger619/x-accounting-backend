package com.unionsg.xaccounting.controller.bankrec;

import com.unionsg.xaccounting.dto.bankrec.AdjustmentResponse;
import com.unionsg.xaccounting.dto.bankrec.AuditLogResponse;
import com.unionsg.xaccounting.dto.bankrec.AutoMatchRequest;
import com.unionsg.xaccounting.dto.bankrec.AutoMatchResultResponse;
import com.unionsg.xaccounting.dto.bankrec.BookTransactionResponse;
import com.unionsg.xaccounting.dto.bankrec.BulkMatchRequest;
import com.unionsg.xaccounting.dto.bankrec.CompleteReconciliationRequest;
import com.unionsg.xaccounting.dto.bankrec.CreateAdjustmentRequest;
import com.unionsg.xaccounting.dto.bankrec.DashboardAccountResponse;
import com.unionsg.xaccounting.dto.bankrec.ManualMatchRequest;
import com.unionsg.xaccounting.dto.bankrec.MatchActionRequest;
import com.unionsg.xaccounting.dto.bankrec.MatchResponse;
import com.unionsg.xaccounting.dto.bankrec.ReasonRequest;
import com.unionsg.xaccounting.dto.bankrec.ReconciliationReportResponse;
import com.unionsg.xaccounting.dto.bankrec.ReconciliationResponse;
import com.unionsg.xaccounting.dto.bankrec.ReconciliationSummaryResponse;
import com.unionsg.xaccounting.dto.bankrec.SaveReconciliationRequest;
import com.unionsg.xaccounting.dto.bankrec.StatementTransactionResponse;
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationStatus;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.bankrec.BankReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bank-reconciliations")
@RequiredArgsConstructor
public class BankReconciliationController {

    private static final String GROUP = "Bank Reconciliation";

    private final BankReconciliationService service;

    @GetMapping
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<Page<ReconciliationResponse>> list(
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) ReconciliationStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "periodEnd", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.list(bankAccountId, status, search, from, to, pageable));
    }

    @GetMapping("/dashboard")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<DashboardAccountResponse>> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @GetMapping("/history")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<ReconciliationResponse>> history(@RequestParam Long bankAccountId) {
        return ResponseEntity.ok(service.history(bankAccountId));
    }

    @GetMapping("/audit")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<AuditLogResponse>> accountAudit(@RequestParam Long bankAccountId) {
        return ResponseEntity.ok(service.accountAudit(bankAccountId));
    }

    @PostMapping
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationResponse> create(@RequestBody SaveReconciliationRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationResponse> update(@PathVariable Long id, @RequestBody SaveReconciliationRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "delete_bank_reconciliation", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/summary")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationSummaryResponse> summary(@PathVariable Long id) {
        return ResponseEntity.ok(service.summary(id));
    }

    @GetMapping("/{id}/report")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationReportResponse> report(@PathVariable Long id) {
        return ResponseEntity.ok(service.report(id));
    }

    @GetMapping("/{id}/audit")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<AuditLogResponse>> audit(@PathVariable Long id) {
        return ResponseEntity.ok(service.audit(id));
    }

    @GetMapping("/{id}/statement-transactions")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<StatementTransactionResponse>> statementTransactions(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        return ResponseEntity.ok(service.statementTransactions(id, status, search, from, to, minAmount, maxAmount));
    }

    @GetMapping("/{id}/book-transactions")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<BookTransactionResponse>> bookTransactions(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        return ResponseEntity.ok(service.bookTransactions(id, status, search, from, to, minAmount, maxAmount));
    }

    @GetMapping("/{id}/matches")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<MatchResponse>> matches(@PathVariable Long id, @RequestParam(required = false) MatchStatus status) {
        return ResponseEntity.ok(service.matches(id, status));
    }

    @PostMapping("/{id}/auto-match")
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<AutoMatchResultResponse> autoMatch(@PathVariable Long id,
                                                             @RequestBody(required = false) AutoMatchRequest request) {
        return ResponseEntity.ok(service.autoMatch(id, request));
    }

    @PostMapping("/{id}/matches")
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<MatchResponse> match(@PathVariable Long id, @RequestBody ManualMatchRequest request) {
        return new ResponseEntity<>(service.manualMatch(id, request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/matches/bulk")
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<MatchResponse>> bulkMatch(@PathVariable Long id, @RequestBody BulkMatchRequest request) {
        return new ResponseEntity<>(service.bulkMatch(id, request.getMatches()), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/matches/confirm")
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<MatchResponse>> confirm(@PathVariable Long id, @RequestBody MatchActionRequest request) {
        return ResponseEntity.ok(service.confirmMatches(id, request));
    }

    @PostMapping("/{id}/matches/reject")
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<MatchResponse>> reject(@PathVariable Long id, @RequestBody MatchActionRequest request) {
        return ResponseEntity.ok(service.rejectMatches(id, request));
    }

    @PostMapping("/{id}/matches/unmatch")
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<MatchResponse>> unmatch(@PathVariable Long id, @RequestBody MatchActionRequest request) {
        return ResponseEntity.ok(service.unmatch(id, request));
    }

    @GetMapping("/{id}/adjustments")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<AdjustmentResponse>> adjustments(@PathVariable Long id) {
        return ResponseEntity.ok(service.adjustments(id));
    }

    @PostMapping("/{id}/adjustments")
    @RequirePermission(value = "post_reconciliation_adjustment", group = GROUP)
    public ResponseEntity<AdjustmentResponse> createAdjustment(@PathVariable Long id, @RequestBody CreateAdjustmentRequest request) {
        return new ResponseEntity<>(service.createAdjustment(id, request), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/adjustments/{adjustmentId}/reverse")
    @RequirePermission(value = "post_reconciliation_adjustment", group = GROUP)
    public ResponseEntity<AdjustmentResponse> reverseAdjustment(@PathVariable Long id, @PathVariable Long adjustmentId,
                                                                @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(service.reverseAdjustment(id, adjustmentId, request.getReason()));
    }

    @PostMapping("/{id}/review")
    @RequirePermission(value = "review_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationResponse> review(@PathVariable Long id, @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(service.review(id, request != null ? request.getReason() : null));
    }

    @PostMapping("/{id}/complete")
    @RequirePermission(value = "complete_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationResponse> complete(@PathVariable Long id,
                                                           @RequestBody(required = false) CompleteReconciliationRequest request) {
        return ResponseEntity.ok(service.complete(id, request));
    }

    @PostMapping("/{id}/reopen")
    @RequirePermission(value = "reopen_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationResponse> reopen(@PathVariable Long id, @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(service.reopen(id, request.getReason()));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(value = "delete_bank_reconciliation", group = GROUP)
    public ResponseEntity<ReconciliationResponse> cancel(@PathVariable Long id, @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(service.cancel(id, request.getReason()));
    }
}
