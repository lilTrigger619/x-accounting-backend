package com.unionsg.xaccounting.event;

import com.unionsg.xaccounting.communication.email.EmailMessage;
import com.unionsg.xaccounting.communication.email.MailService;
import com.unionsg.xaccounting.communication.service.EmailLogService;
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

@Component
@RequiredArgsConstructor
public class InvoiceEmailRequestedEventListener {

    private static final Logger log = LoggerFactory.getLogger(InvoiceEmailRequestedEventListener.class);

    private final InvoiceRepository invoiceRepository;
    private final MailService mailService;
    private final EmailLogService emailLogService;
    private final CustomerActivityLogService customerActivityLogService;

    /**
     * Sends the email using the subject/body already resolved (from the chosen template, or the
     * user's overrides) by InvoiceEmailService at send time — this listener's only job is delivery.
     */
    @Async("emailTaskExecutor")
    @EventListener
    @Transactional
    public void handleInvoiceEmailRequested(InvoiceEmailRequestedEvent event) {
        log.info("Processing email delivery for invoice {} to {}", event.getInvoiceId(), event.getCustomerEmail());

        try {
            Invoice invoice = invoiceRepository.findById(event.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found: " + event.getInvoiceId()));

            Customer customer = invoice.getCustomer();
            if (customer == null) {
                log.warn("Invoice {} has no customer. Skipping email.", event.getInvoiceId());
                return;
            }

            Long emailLogId = emailLogService.createLog(
                    null,
                    "INVOICE",
                    String.valueOf(event.getInvoiceId()),
                    event.getCustomerEmail(),
                    event.getSubject()
            ).getId();

            emailLogService.markSending(emailLogId);

            List<Long> attachmentIds = null;
            if (event.getFileId() != null) {
                try {
                    attachmentIds = List.of(Long.parseLong(event.getFileId()));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse fileId {} as Long for attachment", event.getFileId());
                }
            }

            EmailMessage emailMessage = EmailMessage.builder()
                    .to(event.getCustomerEmail())
                    .subject(event.getSubject())
                    .body(event.getBodyHtml())
                    .html(true)
                    .attachmentFileIds(attachmentIds)
                    .build();

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
                        "Email sent: " + event.getSubject(),
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
