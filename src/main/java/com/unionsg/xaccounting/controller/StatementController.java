package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.statement.StatementResponseDto;
import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.service.statement.StatementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Statements", description = "Customer and supplier running-balance statements")
public class StatementController {

    private final StatementService statementService;

    @GetMapping("/api/customers/{id}/statement")
    @Operation(summary = "Customer statement", description = "Invoices, payments and running balance for a customer over a date range")
    public ResponseEntity<ApiResponse<StatementResponseDto>> customerStatement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(
                ApiResponse.<StatementResponseDto>builder()
                        .success(true)
                        .message("Customer statement retrieved successfully")
                        .content(statementService.getCustomerStatement(id, fromDate, toDate))
                        .build()
        );
    }

    @GetMapping("/api/suppliers/{id}/statement")
    @Operation(summary = "Supplier statement", description = "Bills, payments and running balance for a supplier over a date range")
    public ResponseEntity<ApiResponse<StatementResponseDto>> supplierStatement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(
                ApiResponse.<StatementResponseDto>builder()
                        .success(true)
                        .message("Supplier statement retrieved successfully")
                        .content(statementService.getSupplierStatement(id, fromDate, toDate))
                        .build()
        );
    }
}
