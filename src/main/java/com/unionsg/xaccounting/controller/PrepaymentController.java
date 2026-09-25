package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentRequest;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentListItemResponse;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentResponse;
import com.unionsg.xaccounting.service.prepayment.PrepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prepayments")
@RequiredArgsConstructor
public class PrepaymentController {

    private final PrepaymentService service;

    @PostMapping
    public ResponseEntity<PrepaymentResponse> create(@RequestBody CreatePrepaymentRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrepaymentResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<PrepaymentListItemResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(service.list(pageable));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<PrepaymentResponse> activate(@PathVariable Long id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PostMapping("/{id}/recognize-next")
    public ResponseEntity<PrepaymentResponse> recognizeNext(@PathVariable Long id) {
        return ResponseEntity.ok(service.recognizeNext(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PrepaymentResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id));
    }

    @PostMapping("/{id}/write-off")
    public ResponseEntity<PrepaymentResponse> writeOff(@PathVariable Long id) {
        return ResponseEntity.ok(service.writeOff(id));
    }
}
