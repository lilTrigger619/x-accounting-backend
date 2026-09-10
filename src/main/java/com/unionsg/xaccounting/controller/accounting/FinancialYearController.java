package com.unionsg.xaccounting.controller.accounting;

import com.unionsg.xaccounting.dto.accounting.CreateFinancialYearRequest;
import com.unionsg.xaccounting.dto.accounting.FinancialYearResponse;
import com.unionsg.xaccounting.service.accounting.FinancialYearService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financial-years")
@RequiredArgsConstructor
public class FinancialYearController {

    private final FinancialYearService financialYearService;

    @PostMapping
    public ResponseEntity<FinancialYearResponse> create(@RequestBody CreateFinancialYearRequest request) {
        return new ResponseEntity<>(financialYearService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FinancialYearResponse>> getAll() {
        return ResponseEntity.ok(financialYearService.getAll());
    }

    @GetMapping("/current")
    public ResponseEntity<FinancialYearResponse> getCurrent() {
        return ResponseEntity.ok(financialYearService.getCurrent());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancialYearResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(financialYearService.getById(id));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<FinancialYearResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(financialYearService.activate(id));
    }
}
