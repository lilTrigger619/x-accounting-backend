package com.unionsg.xaccounting.dto.customer;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class CustomerResponseDTO {
    private Long id;
    private String customerCode;
    private String customerType;
    private String displayName;
    private String status;
    private String email;
    private String phone;
    private String mobile;
    private String website;
    private LocalDateTime createdAt;
    private AddressDTO billingAddress;
    private AddressDTO shippingAddress;
    private PaymentTermsDTO paymentTerms;
    private TaxInfoDTO taxInfo;
}
