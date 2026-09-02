package com.unionsg.xaccounting.service.invoice;

import com.unionsg.xaccounting.document.service.InvoiceDocumentService;
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
    private final ApplicationEventPublisher eventPublisher;
    private final CustomerActivityLogService customerActivityLogService;

    /**
     * Sends an invoice via email.
     * Flow:
     * 1. Validate invoice is in DRAFT status
     * 2. Generate PDF via InvoiceDocumentService
     * 3. Set invoice status to SENT
     * 4. Publish async event for email delivery
     */
    @Transactional
    public void sendInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with id: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessException("Only draft invoices can be sent.");
        }

        // Generate PDF via InvoiceDocumentService (handles template lookup)
        String fileId = null;
        try {
            var response = invoiceDocumentService.generateInvoicePdf(invoiceId);
            fileId = response.getFileId();
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}: {}", invoiceId, e.getMessage());
            throw new BusinessException("Failed to generate invoice PDF: " + e.getMessage());
        }

        // Update invoice status to SENT
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setSentAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        // Get customer email
        Customer customer = invoice.getCustomer();
        String customerEmail = customer != null ? customer.getEmail() : null;

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

        if (customerEmail == null || customerEmail.isBlank()) {
            log.warn("Invoice {} has no customer email. Invoice sent but no email will be delivered.", invoiceId);
            return;
        }

        // Publish async event for email delivery
        eventPublisher.publishEvent(new InvoiceEmailRequestedEvent(
                this,
                invoiceId,
                customerEmail,
                fileId
        ));

        log.info("Invoice {} queued for email delivery to {}", invoiceId, customerEmail);
    }
}

