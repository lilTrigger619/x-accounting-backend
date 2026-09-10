package com.unionsg.xaccounting.controller.accounting;

import com.unionsg.xaccounting.dto.accounting.AccountingPeriodResponse;
import com.unionsg.xaccounting.dto.accounting.PeriodActionRequest;
import com.unionsg.xaccounting.service.accounting.AccountingPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting-periods")
@RequiredArgsConstructor
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;

    @GetMapping
    public ResponseEntity<List<AccountingPeriodResponse>> getByFinancialYear(
            @RequestParam Long financialYearId
    ) {
        return ResponseEntity.ok(accountingPeriodService.getByFinancialYear(financialYearId));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<AccountingPeriodResponse> lock(
            @PathVariable Long id,
            @RequestBody(required = false) PeriodActionRequest request
    ) {
        return ResponseEntity.ok(accountingPeriodService.lock(id, request != null ? request.getReason() : null));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<AccountingPeriodResponse> unlock(
            @PathVariable Long id,
            @RequestBody(required = false) PeriodActionRequest request
    ) {
        return ResponseEntity.ok(accountingPeriodService.unlock(id, request != null ? request.getReason() : null));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<AccountingPeriodResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(accountingPeriodService.close(id));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<AccountingPeriodResponse> reopen(
            @PathVariable Long id,
            @RequestBody PeriodActionRequest request
    ) {
        return ResponseEntity.ok(accountingPeriodService.reopen(id, request.getReason()));
    }
}
