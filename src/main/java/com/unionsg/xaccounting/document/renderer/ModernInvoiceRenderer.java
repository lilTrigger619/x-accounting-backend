package com.unionsg.xaccounting.document.renderer;

import org.springframework.stereotype.Component;

@Component
public class ModernInvoiceRenderer implements DocumentRenderer {

    @Override
    public String getTemplatePath() {
        return "documents/invoice/modern";
    }
}

