package com.unionsg.xaccounting.controller.bankrec;

import com.unionsg.xaccounting.dto.bankrec.StatementImportOptions;
import com.unionsg.xaccounting.dto.bankrec.StatementImportResponse;
import com.unionsg.xaccounting.dto.bankrec.StatementTransactionResponse;
import com.unionsg.xaccounting.enums.bankrec.StatementTransactionStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.security.annotation.RequirePermission;
import com.unionsg.xaccounting.service.bankrec.BankStatementImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** Bank statement imports and the imported lines, kept apart from the ledger. */
@RestController
@RequestMapping("/api/bank-statements")
@RequiredArgsConstructor
public class BankStatementController {

    private static final String GROUP = "Bank Reconciliation";

    private final BankStatementImportService service;

    /** Multipart: {@code file} plus the {@link StatementImportOptions} fields as form fields. */
    @PostMapping(value = "/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission(value = "create_bank_reconciliation", group = GROUP)
    public ResponseEntity<StatementImportResponse> importStatement(@RequestParam("file") MultipartFile file,
                                                                   @ModelAttribute StatementImportOptions options) {
        String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("The statement file could not be read");
        }
        StatementImportResponse result = service.importStatement(content, file.getOriginalFilename(), options);
        return new ResponseEntity<>(result, Boolean.TRUE.equals(options.getDryRun()) ? HttpStatus.OK : HttpStatus.CREATED);
    }

    @GetMapping("/imports")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<StatementImportResponse>> imports(@RequestParam(required = false) Long bankAccountId) {
        return ResponseEntity.ok(service.listImports(bankAccountId));
    }

    @GetMapping("/imports/{id}/transactions")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<StatementTransactionResponse>> importTransactions(@PathVariable Long id) {
        return ResponseEntity.ok(service.importTransactions(id));
    }

    @DeleteMapping("/imports/{id}")
    @RequirePermission(value = "delete_bank_reconciliation", group = GROUP)
    public ResponseEntity<Void> deleteImport(@PathVariable Long id) {
        service.deleteImport(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions")
    @RequirePermission(value = "view_bank_reconciliation", group = GROUP)
    public ResponseEntity<List<StatementTransactionResponse>> transactions(
            @RequestParam Long bankAccountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) StatementTransactionStatus status,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.listTransactions(bankAccountId, from, to, status, search));
    }
}
