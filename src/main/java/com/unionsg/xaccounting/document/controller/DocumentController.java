package com.unionsg.xaccounting.document.controller;

import com.unionsg.xaccounting.document.dto.DocumentGenerateRequest;
import com.unionsg.xaccounting.document.dto.DocumentGenerateResponse;
import com.unionsg.xaccounting.document.dto.DocumentPreviewRequest;
import com.unionsg.xaccounting.document.service.InvoiceDocumentService;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for document generation and preview.
 */
@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final InvoiceDocumentService invoiceDocumentService;

    // =============================
    // Preview API
    // =============================

    /**
     * Preview a document using the specified template.
     * Does NOT save anything — no GeneratedDocument, no File, no modifications.
     */
    @PostMapping("/api/document-templates/{templateId}/preview")
    public ResponseEntity<byte[]> previewDocument(
            @PathVariable Long templateId,
            @Valid @RequestBody DocumentPreviewRequest request
    ) {
        byte[] pdfBytes;

        if (request.getDocumentType() == DocumentType.INVOICE) {
            pdfBytes = invoiceDocumentService.previewInvoicePdf(request.getEntityId(), templateId);
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"preview_" + request.getDocumentType().name().toLowerCase() + "_" + request.getEntityId() + ".pdf\"")
                .body(pdfBytes);
    }

    // =============================
    // Generate API
    // =============================

    /**
     * Generate a document PDF and save it via the File Upload module.
     * Returns the file ID and metadata.
     */
    @PostMapping("/api/documents/generate")
    public ResponseEntity<DocumentGenerateResponse> generateDocument(
            @Valid @RequestBody DocumentGenerateRequest request
    ) {
        DocumentGenerateResponse response;

        if (request.getDocumentType() == DocumentType.INVOICE) {
            response = invoiceDocumentService.generateInvoicePdf(request.getEntityId());
        } else {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(response);
    }
}

