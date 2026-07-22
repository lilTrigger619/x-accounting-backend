package com.unionsg.xaccounting.controller;


import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.dto.invoice.CreateInvoiceRequest;
import com.unionsg.xaccounting.dto.invoice.InvoiceResponse;
import com.unionsg.xaccounting.dto.invoice.InvoiceTotalsResponse;
import com.unionsg.xaccounting.dto.invoice.UpdateInvoiceRequest;

import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.service.invoice.InvoiceEmailService;
import com.unionsg.xaccounting.service.invoice.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceEmailService invoiceEmailService;


    /*
     =============================
     Create Invoice
     =============================
     */

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InvoiceResponse> create(

            @RequestPart("files") MultipartFile[] files,
            @RequestPart
            CreateInvoiceRequest request
    ) {

//        System.out.println("Rewquest part "+ request.getInvoiceNumber());
        return ResponseEntity.ok(
                invoiceService.createInvoice(files, request)
        );
    }


    /*
     =============================
     Update
     =============================
     */

    @PutMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> update(

            @PathVariable Long invoiceId,
            @Valid @RequestBody UpdateInvoiceRequest request
    ) {

        return ResponseEntity.ok(
                invoiceService.updateInvoice(invoiceId, request)
        );
    }


    /*
     =============================
     Get By Id
     =============================
     */

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(

            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                invoiceService.getInvoice(id)
        );
    }


    /*
     =============================
     List
     =============================
     */

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> list(

            Pageable pageable
    ) {

        return ResponseEntity.ok(
                invoiceService.getInvoices(pageable)
        );
    }


    /*
     =============================
     Delete
     =============================
     */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(

            @PathVariable Long id
    ) {

        invoiceService.deleteInvoice(id);

        return ResponseEntity.noContent().build();
    }


    /*
     =============================
     Send Invoice
     =============================
     */

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> send(

            @PathVariable Long id
    ) {

        invoiceEmailService.sendInvoice(id);

        return ResponseEntity.ok().build();
    }


    /*
     =============================
     Mark Paid
     =============================
     */

    @PostMapping("/{id}/paid")
    public ResponseEntity<InvoiceResponse> markPaid(

            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                invoiceService.markAsPaid(id)
        );
    }


    /*
     =============================
     Cancel
     =============================
     */

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponse> cancel(

            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                invoiceService.cancelInvoice(id)
        );
    }

    /*
     =============================
     Invoice Totals (Aggregations)
     =============================
     */
    @GetMapping("/summary")
    public ResponseEntity<InvoiceTotalsResponse> summary() {
        return ResponseEntity.ok(invoiceService.getInvoiceTotals());
    }

}
