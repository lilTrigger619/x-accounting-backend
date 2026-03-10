package com.unionsg.xaccounting.dto.customer;

import lombok.Data;
import java.math.BigDecimal;
//import java.security.Identity;


@Data
public class CreateCustomerRequestDTO {
    // Identity
    private String customerType;
    private String title;
    private String firstName;
    private String lastName;
    private String companyName;
    private String displayName;
    private String status;

    // contact
    private String email;
    private String emaill;
    private String phone;
    private String mobile;
    private String website;

    // Billing Address
    private AddressDTO billingAddress;

    // shipping Address
    private AddressDTO shippingAddress;

    // Tax info
    private TaxInfoDTO taxInfo;

    // payment Terms;
    private PaymentTermsDTO paymentTerms;
}
