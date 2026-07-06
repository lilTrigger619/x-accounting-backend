package com.unionsg.xaccounting.controller;

//public class AccountController {
//}
import com.unionsg.xaccounting.dto.AccountCreationDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.unionsg.xaccounting.dto.AccountDTO;
import com.unionsg.xaccounting.service.AccountCommandService;
import com.unionsg.xaccounting.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountCommandService accountCommandService;
    private final AccountService accountService;


    @PostMapping
    public ResponseEntity<AccountCreationDTO> createAccount(@RequestBody AccountCreationDTO accountDTO) {
        AccountCreationDTO created = accountCommandService.createAccount(accountDTO);

        return new ResponseEntity(created, HttpStatus.CREATED);
    }

    //    @GetMapping("/api/v1/accounts")
    @GetMapping("/v1")
    public org.springframework.http.ResponseEntity<org.springframework.data.domain.Page<com.unionsg.xaccounting.dto.AccountListResponse>> getAccounts(

            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,

            @org.springframework.web.bind.annotation.RequestParam(required = false) com.unionsg.xaccounting.enums.AccountType accountType,
            @org.springframework.web.bind.annotation.RequestParam(required = false) com.unionsg.xaccounting.enums.AccountStatus status,
            org.springframework.data.domain.Pageable pageable
    ) {
        return org.springframework.http.ResponseEntity.ok(accountService.getAccounts(search, accountType, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
        AccountDTO account = accountCommandService.getAccountById(id);

        return ResponseEntity.ok(account);
    }

    @GetMapping
    public ResponseEntity<List<AccountDTO>> getAllAccounts() {
        List<AccountDTO> accounts = accountCommandService.getAllAccounts();

        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountDTO> updateAccount(@PathVariable Long id, @RequestBody AccountDTO accountDTO) {
        AccountDTO updated = accountCommandService.updateAccount(id, accountDTO);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        accountCommandService.deleteAccount(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/soft-delete")
    public ResponseEntity<AccountDTO> softDeleteAccount(@PathVariable Long id) {
        AccountDTO deleted = accountCommandService.softDeleteAccount(id);
        return ResponseEntity.ok(deleted);
    }

}