package com.unionsg.xaccounting.document.renderer;

import org.springframework.stereotype.Component;

@Component
public class ProfessionalInvoiceRenderer implements DocumentRenderer {

    @Override
    public String getTemplatePath() {
        return "documents/invoice/professional";
    }
}

