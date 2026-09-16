package com.unionsg.xaccounting.dto.settings;

import lombok.Data;

@Data
public class UpdateOrganizationRequest {
    private String legalName;
    private String tradingName;
    private String registrationNumber;
    private String taxId;
    private String businessType;
    private String industry;
    private String description;
    private String phone;
    private String email;
    private String website;
    private String logoFileId;

    private String primaryAddressLine1;
    private String primaryAddressLine2;
    private String primaryCity;
    private String primaryState;
    private String primaryPostalCode;
    private String primaryCountry;

    private Boolean billingSameAsPrimary;
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingPostalCode;
    private String billingCountry;

    private Boolean shippingSameAsPrimary;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;
}
