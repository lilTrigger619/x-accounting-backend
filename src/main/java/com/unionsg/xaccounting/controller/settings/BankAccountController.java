package com.unionsg.xaccounting.controller.settings;

import com.unionsg.xaccounting.dto.settings.BankAccountResponse;
import com.unionsg.xaccounting.dto.settings.SaveBankAccountRequest;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.settings.BankAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settings/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @RequirePermission(value = "view_settings", group = "Settings")
    public ResponseEntity<List<BankAccountResponse>> list() {
        return ResponseEntity.ok(bankAccountService.list());
    }

    @PostMapping
    @RequirePermission(value = "manage_bank_accounts", group = "Settings")
    public ResponseEntity<BankAccountResponse> create(@RequestBody SaveBankAccountRequest request) {
        return new ResponseEntity<>(bankAccountService.create(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @RequirePermission(value = "manage_bank_accounts", group = "Settings")
    public ResponseEntity<BankAccountResponse> update(@PathVariable Long id, @RequestBody SaveBankAccountRequest request) {
        return ResponseEntity.ok(bankAccountService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(value = "manage_bank_accounts", group = "Settings")
    public ResponseEntity<BankAccountResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.toggleStatus(id));
    }
}
