package com.unionsg.xaccounting.controller;

//public class AccountController {
//}
import com.unionsg.xaccounting.dto.AccountCreationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.unionsg.xaccounting.service.AccountService;
import com.unionsg.xaccounting.dto.AccountDTO;


@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountCreationDTO> createAccount(@RequestBody AccountCreationDTO accountDTO) {
        AccountCreationDTO created = accountService.createAccount(accountDTO);
        return new ResponseEntity(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
        AccountDTO account = accountService.getAccountById(id);
        return ResponseEntity.ok(account);
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        List<AccountDTO> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDTO> updateAccount(@PathVariable Long id, @RequestBody AccountDTO accountDTO) {
        AccountDTO updated = accountService.updateAccount(id, accountDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/soft-delete")
    public ResponseEntity<AccountDTO> softDeleteAccount(@PathVariable Long id) {
        AccountDTO deleted = accountService.softDeleteAccount(id);
        return ResponseEntity.ok(deleted);
    }
}