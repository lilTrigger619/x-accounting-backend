package com.unionsg.xaccounting.documenttemplate.dto.request;

import lombok.Data;

@Data
public class UpdateContentRequest {

    private String formTitle;

    private Boolean showCompanyName;

    private Boolean showPhone;

    private Boolean showEmail;

    private Boolean showWebsite;

    private Boolean showAddress;

    private Boolean showBillingAddress;

    private Boolean showShippingAddress;

    private Boolean showTerms;

    private Boolean showDueDate;

    private Boolean showPaymentMethod;
}

