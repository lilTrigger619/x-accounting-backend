package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.deposit.*;
import com.unionsg.xaccounting.enums.deposit.DepositCounterpartyType;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import com.unionsg.xaccounting.enums.deposit.DepositStatus;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.deposit.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/deposits")
@RequiredArgsConstructor
public class DepositController {

    private static final String GROUP = "Deposits";

    private final DepositService service;

    @PostMapping
    @RequirePermission(value = "manage_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> create(@RequestBody CreateDepositRequest request,
                                                  @RequestParam(defaultValue = "false") boolean activate) {
        return ResponseEntity.ok(service.create(request, activate));
    }

    @GetMapping
    @RequirePermission(value = "view_deposits", group = GROUP)
    public ResponseEntity<Page<DepositListItemResponse>> list(
            @RequestParam(required = false) DepositDirection direction,
            @RequestParam(required = false) DepositStatus status,
            @RequestParam(required = false) DepositCounterpartyType counterpartyType,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long depositTypeId,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Boolean openOnly,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.list(direction, status, counterpartyType, customerId, supplierId, employeeId,
                depositTypeId, currency, search, fromDate, toDate, openOnly, pageable));
    }

    @GetMapping("/dashboard")
    @RequirePermission(value = "view_deposits", group = GROUP)
    public ResponseEntity<DepositDashboardResponse> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @GetMapping("/activity")
    @RequirePermission(value = "view_deposits", group = GROUP)
    public ResponseEntity<Page<DepositActivityResponse>> activity(
            @RequestParam(required = false) DepositDirection direction,
            @RequestParam(required = false) String event,
            @RequestParam(required = false) Long depositId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable
    ) {
        return ResponseEntity.ok(service.activity(direction, event, depositId, fromDate, toDate, pageable));
    }

    @GetMapping("/statement")
    @RequirePermission(value = "view_deposits", group = GROUP)
    public ResponseEntity<DepositStatementResponse> statement(
            @RequestParam(required = false) Long depositId,
            @RequestParam(required = false) DepositCounterpartyType counterpartyType,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String counterpartyName,
            @RequestParam(required = false) DepositDirection direction,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(service.statement(depositId, counterpartyType, customerId, supplierId, employeeId,
                counterpartyName, direction, currency, fromDate, toDate));
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> update(@PathVariable Long id, @RequestBody CreateDepositRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_deposits", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @RequirePermission(value = "manage_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission(value = "manage_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @PostMapping("/{id}/reverse")
    @RequirePermission(value = "reverse_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> reverse(@PathVariable Long id, @RequestBody ReverseDepositRequest request) {
        return ResponseEntity.ok(service.reverse(id, request.getReason()));
    }

    @GetMapping("/{id}/open-documents")
    @RequirePermission(value = "view_deposits", group = GROUP)
    public ResponseEntity<List<DepositOpenDocumentResponse>> openDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(service.openDocuments(id));
    }

    @PostMapping("/{id}/apply")
    @RequirePermission(value = "allocate_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> apply(@PathVariable Long id, @RequestBody ApplyDepositRequest request) {
        return ResponseEntity.ok(service.applyToDocuments(id, request));
    }

    @PostMapping("/{id}/refund")
    @RequirePermission(value = "allocate_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> refund(@PathVariable Long id, @RequestBody RefundDepositRequest request) {
        return ResponseEntity.ok(service.refund(id, request));
    }

    @PostMapping("/{id}/forfeit")
    @RequirePermission(value = "allocate_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> forfeit(@PathVariable Long id, @RequestBody ForfeitDepositRequest request) {
        return ResponseEntity.ok(service.forfeit(id, request));
    }

    @PostMapping("/{id}/transfer")
    @RequirePermission(value = "allocate_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> transfer(@PathVariable Long id, @RequestBody TransferDepositRequest request) {
        return ResponseEntity.ok(service.transfer(id, request));
    }

    @PostMapping("/{id}/allocations/{allocationId}/reverse")
    @RequirePermission(value = "reverse_deposits", group = GROUP)
    public ResponseEntity<DepositResponse> reverseAllocation(@PathVariable Long id, @PathVariable Long allocationId,
                                                             @RequestBody ReverseDepositRequest request) {
        return ResponseEntity.ok(service.reverseAllocation(id, allocationId, request.getReason()));
    }
}
