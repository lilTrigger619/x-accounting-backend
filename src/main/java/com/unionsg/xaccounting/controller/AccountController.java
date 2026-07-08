package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.AccountCreationDTO;
import com.unionsg.xaccounting.dto.AccountDTO;
import com.unionsg.xaccounting.dto.AccountListResponse;
import com.unionsg.xaccounting.service.AccountCommandService;
import com.unionsg.xaccounting.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountCommandService accountCommandService;
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountCreationDTO> createAccount(@RequestBody AccountCreationDTO accountDTO) {
        AccountCreationDTO created = accountCommandService.createAccount(accountDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Required endpoint: GET /api/v1/accounts
    @GetMapping("/api/v1/accounts")
    public ResponseEntity<Page<AccountListResponse>> getAccounts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.unionsg.xaccounting.enums.AccountType accountType,
            @RequestParam(required = false) com.unionsg.xaccounting.enums.AccountStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.getAccounts(search, accountType, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(accountCommandService.getAccountById(id));
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        return ResponseEntity.ok(accountCommandService.getAllAccounts());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDTO> updateAccount(@PathVariable Long id, @RequestBody AccountDTO accountDTO) {
        return ResponseEntity.ok(accountCommandService.updateAccount(id, accountDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountCommandService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/soft-delete")
    public ResponseEntity<AccountDTO> softDeleteAccount(@PathVariable Long id) {
        return ResponseEntity.ok(accountCommandService.softDeleteAccount(id));
    }
}

