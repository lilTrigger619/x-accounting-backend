package com.unionsg.xaccounting.event;

import lombok.Getter;

@Getter
public class InvoiceEmailRequestedEvent {

    private final Object source;
    private final Long invoiceId;
    private final String customerEmail;
    private final String fileId;
    private final String subject;
    private final String bodyHtml;

    public InvoiceEmailRequestedEvent(Object source, Long invoiceId, String customerEmail, String fileId, String subject, String bodyHtml) {
        this.source = source;
        this.invoiceId = invoiceId;
        this.customerEmail = customerEmail;
        this.fileId = fileId;
        this.subject = subject;
        this.bodyHtml = bodyHtml;
    }
}

