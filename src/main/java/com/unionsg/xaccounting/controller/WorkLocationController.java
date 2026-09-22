package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreateWorkLocationRequest;
import com.unionsg.xaccounting.dto.payroll.WorkLocationResponse;
import com.unionsg.xaccounting.service.payroll.WorkLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/work-locations")
@RequiredArgsConstructor
public class WorkLocationController {

    private final WorkLocationService workLocationService;

    @PostMapping
    public ResponseEntity<WorkLocationResponse> create(@Valid @RequestBody CreateWorkLocationRequest request) {
        return ResponseEntity.ok(workLocationService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkLocationResponse> update(@PathVariable Long id, @Valid @RequestBody CreateWorkLocationRequest request) {
        return ResponseEntity.ok(workLocationService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<WorkLocationResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(workLocationService.setActive(id, active));
    }

    @GetMapping
    public ResponseEntity<List<WorkLocationResponse>> getAll() {
        return ResponseEntity.ok(workLocationService.getAll());
    }
}
