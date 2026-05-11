package com.unionsg.xaccounting.dto.invoice;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvoiceBillingInfoRequest {

    private String billingName;

    private String billingEmail;

    private String billingPhone;

    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String postalCode;

    private String country;

}
