package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreateSalaryStructureRequest;
import com.unionsg.xaccounting.dto.payroll.SalaryStructureResponse;
import com.unionsg.xaccounting.service.payroll.SalaryStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/salary-structures")
@RequiredArgsConstructor
public class SalaryStructureController {

    private final SalaryStructureService salaryStructureService;

    @PostMapping
    public ResponseEntity<SalaryStructureResponse> create(@Valid @RequestBody CreateSalaryStructureRequest request) {
        return ResponseEntity.ok(salaryStructureService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SalaryStructureResponse>> getAll() {
        return ResponseEntity.ok(salaryStructureService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaryStructureResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salaryStructureService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SalaryStructureResponse> update(@PathVariable Long id, @Valid @RequestBody CreateSalaryStructureRequest request) {
        return ResponseEntity.ok(salaryStructureService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<SalaryStructureResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(salaryStructureService.setActive(id, active));
    }
}
