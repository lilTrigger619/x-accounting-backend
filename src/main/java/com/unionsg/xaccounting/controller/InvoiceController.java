package com.unionsg.xaccounting.controller;


import com.unionsg.xaccounting.dto.FileUploadRequestDto;
import com.unionsg.xaccounting.dto.invoice.CreateInvoiceRequest;
import com.unionsg.xaccounting.dto.invoice.InvoiceResponse;
import com.unionsg.xaccounting.service.FileService.FileService;
import com.unionsg.xaccounting.service.invoice.InvoiceService;
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

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> update(

            @PathVariable Long id,
            @RequestBody CreateInvoiceRequest request
    ) {

        return ResponseEntity.ok(
                invoiceService.updateInvoice(id, request)
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
    public ResponseEntity<InvoiceResponse> send(

            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                invoiceService.sendInvoice(id)
        );
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

}
