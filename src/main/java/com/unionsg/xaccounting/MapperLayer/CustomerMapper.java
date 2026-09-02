package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.customer.AddressDTO;
import com.unionsg.xaccounting.dto.customer.CreateCustomerRequestDTO;
import com.unionsg.xaccounting.dto.customer.CustomerResponseDTO;
import com.unionsg.xaccounting.dto.customer.PaymentTermsDTO;
import com.unionsg.xaccounting.dto.customer.TaxInfoDTO;
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
                .phone(customer.getPhone())
                .mobile(customer.getMobile())
                .website(customer.getWebsite())
                .createdAt(customer.getCreatedAt())
                .billingAddress(toAddressDto(customer.getBillingAddress()))
                .shippingAddress(toAddressDto(customer.getShippingAddress()))
                .paymentTerms(toPaymentTermsDto(customer.getPaymentTerms()))
                .taxInfo(toTaxInfoDto(customer.getTaxInfo()))
                .build();
    }

    private static AddressDTO toAddressDto(Address address) {
        if (address == null) {
            return null;
        }
        return AddressDTO.builder()
                .addressLine(address.getAddressLine())
                .city(address.getCity())
                .state(address.getState())
                .zipCode(address.getZipCode())
                .country(address.getCountry())
                .build();
    }

    private static PaymentTermsDTO toPaymentTermsDto(PaymentTerms paymentTerms) {
        if (paymentTerms == null) {
            return null;
        }
        return PaymentTermsDTO.builder()
                .paymentTermType(paymentTerms.getPaymentTermType() != null ? paymentTerms.getPaymentTermType().name() : null)
                .creditLimit(paymentTerms.getCreditLimit())
                .currency(paymentTerms.getCurrency() != null ? paymentTerms.getCurrency().name() : null)
                .build();
    }

    private static TaxInfoDTO toTaxInfoDto(TaxInfo taxInfo) {
        if (taxInfo == null) {
            return null;
        }
        TaxInfoDTO dto = new TaxInfoDTO();
        dto.setTaxId(taxInfo.getTaxId());
        dto.setTaxExempt(taxInfo.getTaxExempt());
        dto.setTaxExemptReason(taxInfo.getTaxExemptReason());
        return dto;
    }
}
