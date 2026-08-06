package com.unionsg.xaccounting.documenttemplate.controller;

import com.unionsg.xaccounting.documenttemplate.dto.request.*;
import com.unionsg.xaccounting.documenttemplate.dto.response.DocumentTemplateResponse;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.documenttemplate.service.DocumentTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/document-templates")
@RequiredArgsConstructor
public class DocumentTemplateController {

    private final DocumentTemplateService documentTemplateService;

    // =============================
    // Create Template
    // =============================

    @PostMapping
    public ResponseEntity<DocumentTemplateResponse> createTemplate(
            @Valid @RequestBody CreateDocumentTemplateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentTemplateService.createTemplate(request));
    }

    // =============================
    // Get Template
    // =============================

    @GetMapping("/{id}")
    public ResponseEntity<DocumentTemplateResponse> getTemplate(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(documentTemplateService.getTemplate(id));
    }

    // =============================
// List Templates
    // =============================

    @GetMapping
    public ResponseEntity<Page<DocumentTemplateResponse>> listTemplates(
            @RequestParam(name = "type", required = false) DocumentType type,
            @RequestParam(name = "systemOnly", defaultValue = "false") boolean systemOnly,
            @RequestParam(name = "userOnly", defaultValue = "false") boolean userOnly,
            Pageable pageable
    ) {
        return ResponseEntity.ok(documentTemplateService.listTemplates(type, systemOnly, userOnly, pageable));
    }

    // =============================
    // Update Template
    // =============================

    @PutMapping("/{id}")
    public ResponseEntity<DocumentTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDocumentTemplateRequest request
    ) {
        return ResponseEntity.ok(documentTemplateService.updateTemplate(id, request));
    }

    // =============================
    // Delete Template
    // =============================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable Long id
    ) {
        documentTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // =============================
    // Update Design
    // =============================

    @PutMapping("/{id}/design")
    public ResponseEntity<DocumentTemplateResponse> updateDesign(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDesignRequest request
    ) {
        return ResponseEntity.ok(documentTemplateService.updateDesign(id, request));
    }

    // =============================
    // Update Content
    // =============================

    @PutMapping("/{id}/content")
    public ResponseEntity<DocumentTemplateResponse> updateContent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContentRequest request
    ) {
        return ResponseEntity.ok(documentTemplateService.updateContent(id, request));
    }

    // =============================
    // Update Standard Email
    // =============================

    @PutMapping("/{id}/emails/standard")
    public ResponseEntity<DocumentTemplateResponse> updateStandardEmail(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailRequest request
    ) {
        return ResponseEntity.ok(documentTemplateService.updateStandardEmail(id, request));
    }

    // =============================
    // Update Reminder Email
    // =============================

    @PutMapping("/{id}/emails/reminder")
    public ResponseEntity<DocumentTemplateResponse> updateReminderEmail(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmailRequest request
    ) {
        return ResponseEntity.ok(documentTemplateService.updateReminderEmail(id, request));
    }

    // =============================
    // Set Default Template
    // =============================

    @PostMapping("/{id}/default")
    public ResponseEntity<DocumentTemplateResponse> setDefaultTemplate(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(documentTemplateService.setDefaultTemplate(id));
    }
}

