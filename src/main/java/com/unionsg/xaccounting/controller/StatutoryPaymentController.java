package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.dto.payroll.RecordStatutoryPaymentRequest;
import com.unionsg.xaccounting.service.payroll.StatutoryPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payroll/statutory-payments")
@RequiredArgsConstructor
public class StatutoryPaymentController {

    private final StatutoryPaymentService statutoryPaymentService;

    @PostMapping
    public ResponseEntity<JournalResponse> recordPayment(@Valid @RequestBody RecordStatutoryPaymentRequest request) {
        return ResponseEntity.ok(statutoryPaymentService.recordPayment(request));
    }
}
