package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentTypeRequest;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentTypeResponse;
import com.unionsg.xaccounting.service.prepayment.PrepaymentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prepayment-types")
@RequiredArgsConstructor
public class PrepaymentTypeController {

    private final PrepaymentTypeService service;

    @PostMapping
    public ResponseEntity<PrepaymentTypeResponse> create(@RequestBody CreatePrepaymentTypeRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PrepaymentTypeResponse>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(activeOnly ? service.listActive() : service.listAll());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
