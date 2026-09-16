package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreateStatutorySchemeRequest;
import com.unionsg.xaccounting.dto.payroll.StatutorySchemeResponse;
import com.unionsg.xaccounting.service.payroll.StatutorySchemeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/statutory-schemes")
@RequiredArgsConstructor
public class StatutorySchemeController {

    private final StatutorySchemeService statutorySchemeService;

    @PostMapping
    public ResponseEntity<StatutorySchemeResponse> create(@Valid @RequestBody CreateStatutorySchemeRequest request) {
        return ResponseEntity.ok(statutorySchemeService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<StatutorySchemeResponse>> getAll() {
        return ResponseEntity.ok(statutorySchemeService.getAll());
    }
}
