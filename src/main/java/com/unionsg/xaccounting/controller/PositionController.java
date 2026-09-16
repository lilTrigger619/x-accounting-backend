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

    @GetMapping
    public ResponseEntity<List<PositionResponse>> getAll() {
        return ResponseEntity.ok(positionService.getAll());
    }
}
