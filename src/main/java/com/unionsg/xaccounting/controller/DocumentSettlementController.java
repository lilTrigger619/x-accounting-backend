package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.settlement.SettlementSummaryResponse;
import com.unionsg.xaccounting.service.settlement.DocumentSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read side of the shared invoice/bill settlement ledger (deposits, downpayments). */
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class DocumentSettlementController {

    private final DocumentSettlementService service;

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<SettlementSummaryResponse> invoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(service.invoiceSummary(invoiceId));
    }

    @GetMapping("/bills/{billId}")
    public ResponseEntity<SettlementSummaryResponse> bill(@PathVariable Long billId) {
        return ResponseEntity.ok(service.billSummary(billId));
    }
}
