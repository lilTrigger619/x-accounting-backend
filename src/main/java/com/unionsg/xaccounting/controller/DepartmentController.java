package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreateDepartmentRequest;
import com.unionsg.xaccounting.dto.payroll.DepartmentResponse;
import com.unionsg.xaccounting.service.payroll.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> update(@PathVariable Long id, @Valid @RequestBody CreateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<DepartmentResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(departmentService.setActive(id, active));
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAll() {
        return ResponseEntity.ok(departmentService.getAll());
    }
}
