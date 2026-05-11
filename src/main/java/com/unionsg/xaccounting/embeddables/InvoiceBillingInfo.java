package com.unionsg.xaccounting.embeddables;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class InvoiceBillingInfo {

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
