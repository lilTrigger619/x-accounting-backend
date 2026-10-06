package com.unionsg.xaccounting.controller.bankrec;

import com.unionsg.xaccounting.dto.bankrec.MatchingRuleRequest;
import com.unionsg.xaccounting.dto.bankrec.MatchingRuleResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.bankrec.BankMatchingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-matching-rules")
@RequiredArgsConstructor
public class BankMatchingRuleController {

    private static final String GROUP = "Bank Reconciliation";

    private final BankMatchingRuleService service;

    @GetMapping
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<MatchingRuleResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<MatchingRuleResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @RequirePermission(value = "manage_bank_reconciliation_setup", group = GROUP)
    public ResponseEntity<MatchingRuleResponse> create(@RequestBody MatchingRuleRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_bank_reconciliation_setup", group = GROUP)
    public ResponseEntity<MatchingRuleResponse> update(@PathVariable Long id, @RequestBody MatchingRuleRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_bank_reconciliation_setup", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
