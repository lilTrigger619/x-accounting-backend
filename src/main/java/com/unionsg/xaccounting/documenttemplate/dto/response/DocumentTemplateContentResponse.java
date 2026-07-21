package com.unionsg.xaccounting.documenttemplate.dto.response;

import lombok.Data;

@Data
public class DocumentTemplateContentResponse {

    private Long id;
    private String formTitle;
    private boolean showCompanyName;
    private boolean showPhone;
    private boolean showEmail;
    private boolean showWebsite;
    private boolean showAddress;
    private boolean showBillingAddress;
    private boolean showShippingAddress;
    private boolean showTerms;
    private boolean showDueDate;
    private boolean showPaymentMethod;
}

