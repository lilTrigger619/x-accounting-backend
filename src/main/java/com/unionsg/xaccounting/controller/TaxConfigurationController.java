package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreateTaxConfigurationRequest;
import com.unionsg.xaccounting.dto.payroll.TaxConfigurationResponse;
import com.unionsg.xaccounting.service.payroll.TaxConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/tax-configurations")
@RequiredArgsConstructor
public class TaxConfigurationController {

    private final TaxConfigurationService taxConfigurationService;

    @PostMapping
    public ResponseEntity<TaxConfigurationResponse> create(@Valid @RequestBody CreateTaxConfigurationRequest request) {
        return ResponseEntity.ok(taxConfigurationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<TaxConfigurationResponse>> getAll() {
        return ResponseEntity.ok(taxConfigurationService.getAll());
    }
}
