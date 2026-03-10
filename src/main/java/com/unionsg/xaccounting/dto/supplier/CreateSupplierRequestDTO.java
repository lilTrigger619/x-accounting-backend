package com.unionsg.xaccounting.dto.supplier;
import com.unionsg.xaccounting.dto.customer.AddressDTO;
import com.unionsg.xaccounting.dto.customer.PaymentTermsDTO;
import com.unionsg.xaccounting.dto.customer.TaxInfoDTO;

import lombok.Data;

@Data
public class CreateSupplierRequestDTO {
    // Identity
    private String supplierType;
//    private String title;
//    private String firstName;
//    private String lastName;
    private String contactPerson;
    private String companyName;
    private String displayName;
    private String status;
    private String category;


    // contact
    private String email;
    private String phone;
    private String mobile;
    private String website;

    //billing Address
    private AddressDTO address;

//    // shipping Address
//    private AddressDTO shippingAddress;

    // Tax info
    private SupplierTaxInfoDTO taxInfo;

    // paymentTerms;
    private SupplierPaymentTermsDTO paymentTerms;

    // additional information
    private String additionalInformation;
}
