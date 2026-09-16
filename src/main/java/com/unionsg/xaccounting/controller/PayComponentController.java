package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreatePayComponentRequest;
import com.unionsg.xaccounting.dto.payroll.PayComponentResponse;
import com.unionsg.xaccounting.service.payroll.PayComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/pay-components")
@RequiredArgsConstructor
public class PayComponentController {

    private final PayComponentService payComponentService;

    @PostMapping
    public ResponseEntity<PayComponentResponse> create(@Valid @RequestBody CreatePayComponentRequest request) {
        return ResponseEntity.ok(payComponentService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PayComponentResponse> update(@PathVariable Long id, @Valid @RequestBody CreatePayComponentRequest request) {
        return ResponseEntity.ok(payComponentService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<PayComponentResponse>> getAll() {
        return ResponseEntity.ok(payComponentService.getAll());
    }
}
