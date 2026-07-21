package com.unionsg.xaccounting.document.service;

import com.unionsg.xaccounting.document.dto.DocumentGenerateResponse;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateRepository;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Bridge between the Invoice module and the Document module.
 * The Document module does NOT know about InvoiceRepository.
 * This service loads the invoice and delegates to DocumentGenerationService.
 */
@Service
@RequiredArgsConstructor
public class InvoiceDocumentService {

    private final InvoiceRepository invoiceRepository;
    private final DocumentTemplateRepository templateRepository;
    private final DocumentGenerationService documentGenerationService;

    /**
     * Generates a PDF invoice for the given invoice ID.
     * Uses the default template for INVOICE document type.
     */
    public DocumentGenerateResponse generateInvoicePdf(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with id: " + invoiceId));

        DocumentTemplate template = templateRepository
                .findByDocumentTypeAndIsDefaultTrue(DocumentType.INVOICE)
                .orElseThrow(() -> new BusinessException("No default invoice template found. Please set a default template first."));

        return documentGenerationService.generate(
                invoice,
                template,
                DocumentType.INVOICE,
                "INVOICE",
                invoiceId
        );
    }

    /**
     * Generates a preview PDF for an invoice using a specific template.
     * Does NOT save anything.
     */
    public byte[] previewInvoicePdf(Long invoiceId, Long templateId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with id: " + invoiceId));

        DocumentTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("Template not found with id: " + templateId));

        return documentGenerationService.preview(invoice, template);
    }

    /**
     * Generates a preview PDF for an invoice using the default template.
     */
    public byte[] previewInvoiceWithDefaultTemplate(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with id: " + invoiceId));

        DocumentTemplate template = templateRepository
                .findByDocumentTypeAndIsDefaultTrue(DocumentType.INVOICE)
                .orElseThrow(() -> new BusinessException("No default invoice template found."));

        return documentGenerationService.preview(invoice, template);
    }
}

