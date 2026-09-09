package com.unionsg.xaccounting.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceSendPreviewResponse {

    private String toEmail;

    private String subject;

    private String bodyHtml;

}
