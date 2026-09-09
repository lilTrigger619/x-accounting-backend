package com.unionsg.xaccounting.dto.invoice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendInvoiceRequest {

    /** Document template to render the invoice PDF and resolve the email content with. Defaults to the default INVOICE template when omitted. */
    private Long templateId;

    /** Overrides the customer's email on file, if provided. */
    private String email;

    /** Overrides the template-resolved subject, if provided. */
    private String subject;

    /** Overrides the template-resolved body, if provided. */
    private String message;

}
