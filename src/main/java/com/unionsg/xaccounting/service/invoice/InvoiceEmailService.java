package com.unionsg.xaccounting.service.invoice;

import com.unionsg.xaccounting.document.service.InvoiceDocumentService;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.dto.invoice.InvoiceSendPreviewResponse;
import com.unionsg.xaccounting.dto.invoice.SendInvoiceRequest;
import com.unionsg.xaccounting.event.InvoiceEmailRequestedEvent;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.CustomerActivityReferenceType;
import com.unionsg.xaccounting.enums.CustomerActivityType;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.service.customer.CustomerActivityLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvoiceEmailService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEmailService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceDocumentService invoiceDocumentService;
    private final InvoiceEmailContentService invoiceEmailContentService;
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerActivityLogService customerActivityLogService;
    private final InvoiceJournalService invoiceJournalService;

    /**
     * Sends an invoice via email.
     * Flow:
     * 1. Validate invoice is in DRAFT status
     * 2. Generate PDF via InvoiceDocumentService, using the chosen template (or the default one)
     * 3. Post the invoice's GL journal (Dr Accounts Receivable, Cr Revenue/Sales Tax Payable) -
     *    the DRAFT check above guarantees this is the invoice's first (and only) send, so there
     *    is no separate "first send" flag to track the way {@link InvoiceService#sendInvoice}
     *    needs one for its own, re-sendable notion of "send".
     * 4. Set invoice status to SENT
     * 5. Resolve the recipient email and email content (subject/body), honoring any overrides
     * 6. Publish async event for email delivery
     */
    @Transactional
    public void sendInvoice(Long invoiceId, SendInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessException("Only draft invoices can be sent.");
        }

        Long templateId = request != null ? request.getTemplateId() : null;
        DocumentTemplate template = invoiceDocumentService.resolveInvoiceTemplate(templateId);

        String fileId;
        try {
            var response = invoiceDocumentService.generateInvoicePdf(invoiceId, templateId);
            fileId = response.getFileId();
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}: {}", invoiceId, e.getMessage());
            throw new BusinessException("Failed to generate invoice PDF: " + e.getMessage());
        }

        invoiceJournalService.postInvoiceJournal(invoice);

        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        Customer customer = invoice.getCustomer();
        String overrideEmail = request != null ? request.getEmail() : null;
        String recipientEmail = (overrideEmail != null && !overrideEmail.isBlank())
                ? overrideEmail.trim()
                : (customer != null ? customer.getEmail() : null);

        if (customer != null) {
            customerActivityLogService.record(
                    customer.getId(),
                    CustomerActivityType.INVOICE_SENT,
                    "Invoice " + invoice.getInvoiceNumber() + " sent",
                    null,
                    CustomerActivityReferenceType.INVOICE,
                    invoice.getId()
            );
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Invoice {} has no recipient email. Invoice sent but no email will be delivered.", invoiceId);
            return;
        }

        InvoiceEmailContent content = invoiceEmailContentService.resolveContent(
                invoice,
                template,
                request != null ? request.getSubject() : null,
                request != null ? request.getMessage() : null
        );

        eventPublisher.publishEvent(new InvoiceEmailRequestedEvent(
                this,
                invoiceId,
                recipientEmail,
                fileId,
                content.getSubject(),
                content.getBodyHtml()
        ));

        log.info("Invoice {} queued for email delivery to {}", invoiceId, recipientEmail);
    }

    /**
     * Resolves what an invoice send would look like (recipient, subject, rendered body) without
     * sending anything, changing invoice status, or generating/saving a PDF.
     */
    @Transactional(readOnly = true)
    public InvoiceSendPreviewResponse previewSend(Long invoiceId, SendInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with id: " + invoiceId));

        Long templateId = request != null ? request.getTemplateId() : null;
        DocumentTemplate template = invoiceDocumentService.resolveInvoiceTemplate(templateId);

        Customer customer = invoice.getCustomer();
        String overrideEmail = request != null ? request.getEmail() : null;
        String toEmail = (overrideEmail != null && !overrideEmail.isBlank())
                ? overrideEmail.trim()
                : (customer != null ? customer.getEmail() : null);

        InvoiceEmailContent content = invoiceEmailContentService.resolveContent(
                invoice,
                template,
                request != null ? request.getSubject() : null,
                request != null ? request.getMessage() : null
        );

        return InvoiceSendPreviewResponse.builder()
                .toEmail(toEmail)
                .subject(content.getSubject())
                .bodyHtml(content.getBodyHtml())
                .build();
    }
}
