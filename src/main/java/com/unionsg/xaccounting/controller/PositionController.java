package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.payroll.CreatePositionRequest;
import com.unionsg.xaccounting.dto.payroll.PositionResponse;
import com.unionsg.xaccounting.service.payroll.PositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll/positions")
@RequiredArgsConstructor
public class PositionController {

    private final PositionService positionService;

    @PostMapping
    public ResponseEntity<PositionResponse> create(@Valid @RequestBody CreatePositionRequest request) {
        return ResponseEntity.ok(positionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PositionResponse> update(@PathVariable Long id, @Valid @RequestBody CreatePositionRequest request) {
        return ResponseEntity.ok(positionService.update(id, request));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<PositionResponse> setActive(@PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(positionService.setActive(id, active));
    }

    @GetMapping
    public ResponseEntity<List<PositionResponse>> getAll() {
        return ResponseEntity.ok(positionService.getAll());
    }
}
