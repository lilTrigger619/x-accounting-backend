package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.customer.CreateCustomerRequestDTO;
import com.unionsg.xaccounting.dto.customer.CustomerResponseDTO;
import com.unionsg.xaccounting.entity.customer.Address;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.entity.customer.TaxInfo;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.enums.AddressType;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.PaymentTermType;
import com.unionsg.xaccounting.enums.Title;
import com.unionsg.xaccounting.enums.CustomerType;
import com.unionsg.xaccounting.enums.CustomerStatus;

public class CustomerMapper {

    public static Customer toEntity(CreateCustomerRequestDTO dto){
         Address billing = Address.builder()
                 .addressLine(dto.getBillingAddress().getAddressLine())
                 .city(dto.getBillingAddress().getCity())
                 .state(dto.getBillingAddress().getState())
                 .zipCode(dto.getBillingAddress().getZipCode())
                 .country(dto.getBillingAddress().getCountry())
                 .addressType(AddressType.BILLING)
                 .build();

         Address shipping = Address.builder()
                 .addressLine(dto.getShippingAddress().getAddressLine())
                 .city(dto.getShippingAddress().getCity())
                 .state(dto.getShippingAddress().getState())
                 .zipCode(dto.getShippingAddress().getZipCode())
                 .country(dto.getShippingAddress().getCountry())
                 .addressType(AddressType.SHIPPING)
                 .build();

         TaxInfo taxInfo = TaxInfo.builder()
                 .taxId(dto.getTaxInfo().getTaxId())
                 .taxExempt(dto.getTaxInfo().getTaxExempt())
                 .taxExemptReason(dto.getTaxInfo().getTaxExemptReason())
                 .build();

         PaymentTerms paymentTerms = PaymentTerms.builder()
                 .paymentTermType(PaymentTermType.valueOf(dto.getPaymentTerms().getPaymentTermType()))
                 .creditLimit(dto.getPaymentTerms().getCreditLimit())
                 .currency(Currency.valueOf(dto.getPaymentTerms().getCurrency()))
                 .build();

         return Customer.builder()
                 .customerType(CustomerType.valueOf(dto.getCustomerType()))
                 .title(dto.getTitle() != null ? Title.valueOf(dto.getTitle()) : null)
                 .firstName(dto.getFirstName())
                 .lastName(dto.getLastName())
                 .companyName(dto.getCompanyName())
                 .displayName(dto.getDisplayName())
                 .status(CustomerStatus.valueOf(dto.getStatus()))
                 .email(dto.getEmail())
                 .phone(dto.getPhone())
                 .mobile(dto.getMobile())
                 .website(dto.getWebsite())
                 .billingAddress(billing)
                 .shippingAddress(shipping)
                 .taxInfo(taxInfo)
                 .paymentTerms(paymentTerms)
                 .build();
    }

    public static CustomerResponseDTO toResponse(Customer customer){
        return CustomerResponseDTO.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .displayName(customer.getDisplayName())
                .customerType(customer.getCustomerType().name())
                .status(customer.getStatus().name())
                .email(customer.getEmail())
                .build();
    }
}
