package com.unionsg.xaccounting.controller;
//
//public class ChartOfAccountController {
//}

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.unionsg.xaccounting.dto.AccountDTO;
import com.unionsg.xaccounting.dto.ChartOfAccountDTO;
import com.unionsg.xaccounting.service.ChartOfAccountService;

@RestController
@RequestMapping("/api/chart-of-accounts")
@RequiredArgsConstructor
public class ChartOfAccountController {

    private final ChartOfAccountService chartOfAccountService;

    @PostMapping
    public ResponseEntity<ChartOfAccountDTO> createChartOfAccount(@RequestBody ChartOfAccountDTO dto) {
        ChartOfAccountDTO created = chartOfAccountService.createChartOfAccount(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChartOfAccountDTO> getChartOfAccountById(@PathVariable Long id) {
        ChartOfAccountDTO chart = chartOfAccountService.getChartOfAccountById(id);
        return ResponseEntity.ok(chart);
    }

    /*
    @GetMapping("/code/{coaCode}")
    public ResponseEntity<ChartOfAccountDTO> getChartOfAccountByCode(@PathVariable Long coaCode) {
        ChartOfAccountDTO chart = chartOfAccountService.getChartOfAccountByCode(coaCode);
        return ResponseEntity.ok(chart);
    }*/

    @GetMapping
    public ResponseEntity<List<ChartOfAccountDTO>> getAllChartOfAccounts() {
        List<ChartOfAccountDTO> charts = chartOfAccountService.getAllChartOfAccounts();
        return ResponseEntity.ok(charts);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChartOfAccountDTO> updateChartOfAccount(@PathVariable Long id, @RequestBody ChartOfAccountDTO dto) {
        ChartOfAccountDTO updated = chartOfAccountService.updateChartOfAccount(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChartOfAccount(@PathVariable Long id) {
        chartOfAccountService.deleteChartOfAccount(id);
        return ResponseEntity.noContent().build();
    }
}