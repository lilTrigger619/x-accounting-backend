package com.unionsg.xaccounting.service.invoice;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InvoiceEmailContent {
    private final String subject;
    private final String bodyHtml;
}
