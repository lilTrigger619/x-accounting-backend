package com.unionsg.xaccounting.controller.bankrec;

import com.unionsg.xaccounting.dto.bankrec.ImportProfileRequest;
import com.unionsg.xaccounting.dto.bankrec.ImportProfileResponse;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.bankrec.BankStatementImportProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-statement-import-profiles")
@RequiredArgsConstructor
public class BankStatementImportProfileController {

    private static final String GROUP = "Bank Reconciliation";

    private final BankStatementImportProfileService service;

    @GetMapping
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<ImportProfileResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{id}")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<ImportProfileResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @RequirePermission(value = "manage_bank_reconciliation_setup", group = GROUP)
    public ResponseEntity<ImportProfileResponse> create(@RequestBody ImportProfileRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_bank_reconciliation_setup", group = GROUP)
    public ResponseEntity<ImportProfileResponse> update(@PathVariable Long id, @RequestBody ImportProfileRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_bank_reconciliation_setup", group = GROUP)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
