package com.unionsg.xaccounting.controller.accounting;

import com.unionsg.xaccounting.dto.accounting.CreateOpeningBalanceRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.service.accounting.OpeningBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/opening-balances")
@RequiredArgsConstructor
public class OpeningBalanceController {

    private final OpeningBalanceService openingBalanceService;

    @PostMapping
    public ResponseEntity<JournalResponse> create(@Valid @RequestBody CreateOpeningBalanceRequest request) {
        return new ResponseEntity<>(openingBalanceService.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{financialYearId}")
    public ResponseEntity<JournalResponse> getForFinancialYear(@PathVariable Long financialYearId) {
        return ResponseEntity.ok(openingBalanceService.getForFinancialYear(financialYearId));
    }
}
