package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.reports.BalanceSheetResponseDTO;
import com.unionsg.xaccounting.service.reports.BalanceSheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class BalanceSheetController {

    private final BalanceSheetService balanceSheetService;

    @GetMapping("/balance-sheet")
    public ResponseEntity<BalanceSheetResponseDTO> getBalanceSheet(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate asOfDate
    ) {
        return ResponseEntity.ok(balanceSheetService.generateReport(asOfDate));
    }
}
