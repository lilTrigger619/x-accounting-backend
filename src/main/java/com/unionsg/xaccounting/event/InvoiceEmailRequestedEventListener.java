package com.unionsg.xaccounting.event;

import com.unionsg.xaccounting.communication.email.EmailMessage;
import com.unionsg.xaccounting.communication.email.MailService;
import com.unionsg.xaccounting.communication.service.EmailLogService;
import com.unionsg.xaccounting.communication.template.EmailTemplateRenderer;
import com.unionsg.xaccounting.communication.template.EmailVariableResolver;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplate;
import com.unionsg.xaccounting.documenttemplate.domain.DocumentTemplateEmail;
import com.unionsg.xaccounting.documenttemplate.enums.DocumentType;
import com.unionsg.xaccounting.documenttemplate.enums.EmailType;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateEmailRepository;
import com.unionsg.xaccounting.documenttemplate.repository.DocumentTemplateRepository;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.enums.CustomerActivityReferenceType;
import com.unionsg.xaccounting.enums.CustomerActivityType;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.service.customer.CustomerActivityLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InvoiceEmailRequestedEventListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEmailRequestedEventListener.class);

    private final InvoiceRepository invoiceRepository;
    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateEmailRepository emailTemplateRepository;
    private final EmailVariableResolver variableResolver;
    private final EmailTemplateRenderer emailRenderer;
    private final MailService mailService;
    private final EmailLogService emailLogService;
    private final CustomerActivityLogService customerActivityLogService;

    @Async("emailTaskExecutor")
    @EventListener
    @Transactional
    public void handleInvoiceEmailRequested(InvoiceEmailRequestedEvent event) {
        log.info("Processing email delivery for invoice {} to {}", event.getInvoiceId(), event.getCustomerEmail());

        try {
            // Load invoice
            Invoice invoice = invoiceRepository.findById(event.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + event.getInvoiceId()));

            Customer customer = invoice.getCustomer();
            if (customer == null) {
                log.warn("Invoice {} has no customer. Skipping email.", event.getInvoiceId());
                return;
            }

            // Load invoice template (first available)
            List<DocumentTemplate> templates = templateRepository.findByDocumentType(DocumentType.INVOICE);
            DocumentTemplate template = templates.isEmpty() ? null : templates.get(0);

            // Build variables
            String companyName = "Your Company"; // TODO: resolve from CompanyInfoResolver
            Map<String, Object> variables = variableResolver.buildInvoiceVariables(
                    invoice.getInvoiceNumber(),
                    invoice.getIssueDate(),
                    invoice.getDueDate(),
                    customer.getDisplayName() != null ? customer.getDisplayName() : (customer.getFirstName() + " " + customer.getLastName()),
                    customer.getFirstName(),
                    customer.getLastName(),
                    companyName,
                    invoice.getTotalAmount(),
                    invoice.getTotalDue(),
                    "Net 30" // TODO: resolve from payment terms
            );

            // Find STANDARD email template
            DocumentTemplateEmail emailTemplate = null;
            if (template != null) {
                emailTemplate = emailTemplateRepository
                        .findByTemplateAndEmailType(template, EmailType.STANDARD)
                        .orElse(null);
            }

            // Create email log
            Long emailLogId = emailLogService.createLog(
                    null,
                    "INVOICE",
                    String.valueOf(event.getInvoiceId()),
                    event.getCustomerEmail(),
                    emailTemplate != null ? emailTemplate.getSubject() : ("Invoice " + invoice.getInvoiceNumber())
            ).getId();

            // Mark as sending
            emailLogService.markSending(emailLogId);

            // Resolve subject and body
            String subject;
            String bodyHtml;

            if (emailTemplate != null) {
                subject = emailRenderer.resolveSubject(emailTemplate, variables);
                String salutation = emailRenderer.resolveSalutation(emailTemplate, variables);
                variables.put("salutation", salutation);
                bodyHtml = emailRenderer.renderBody("email/invoice-standard", variables);
            } else {
                subject = "Invoice " + invoice.getInvoiceNumber();
                bodyHtml = "<p>Dear " + variables.get("customerName") + ",</p>"
                        + "<p>Please find attached invoice " + invoice.getInvoiceNumber() + " for your reference.</p>"
                        + "<p>Total Amount: " + variables.get("total") + "</p>"
                        + "<p>Due Date: " + variables.get("dueDate") + "</p>";
            }

            // Parse fileId to Long for EmailMessage
            List<Long> attachmentIds = null;
            if (event.getFileId() != null) {
                try {
                    attachmentIds = List.of(Long.parseLong(event.getFileId()));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse fileId {} as Long for attachment", event.getFileId());
                }
            }

            // Build email message
            EmailMessage emailMessage = EmailMessage.builder()
                    .to(event.getCustomerEmail())
                    .subject(subject)
                    .body(bodyHtml)
                    .html(true)
                    .attachmentFileIds(attachmentIds)
                    .build();

            // Send email
            var result = mailService.send(emailMessage);

            if (result.isSuccess()) {
                emailLogService.markSent(emailLogId, result.getProviderMessageId());
                if (attachmentIds != null) {
                    for (Long fileId : attachmentIds) {
                        emailLogService.addAttachment(emailLogId, fileId);
                    }
                }
                customerActivityLogService.record(
                        customer.getId(),
                        CustomerActivityType.EMAIL_SENT,
                        "Email sent: " + subject,
                        "To " + event.getCustomerEmail(),
                        CustomerActivityReferenceType.INVOICE,
                        invoice.getId()
                );
                log.info("Email sent successfully for invoice {} to {}", event.getInvoiceId(), event.getCustomerEmail());
            } else {
                emailLogService.markFailed(emailLogId, result.getFailedReason());
                log.error("Failed to send email for invoice {}: {}", event.getInvoiceId(), result.getFailedReason());
            }

        } catch (Exception e) {
            log.error("Error processing email for invoice {}: {}", event.getInvoiceId(), e.getMessage(), e);
        }
    }
}

