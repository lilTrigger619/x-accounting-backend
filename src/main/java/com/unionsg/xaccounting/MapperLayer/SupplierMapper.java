package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.supplier.CreateSupplierRequestDTO;
import com.unionsg.xaccounting.dto.supplier.SupplierResponseDTO;
import com.unionsg.xaccounting.entity.customer.Address;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.entity.customer.TaxInfo;
import com.unionsg.xaccounting.entity.customer.Customer;


import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.AddressType;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.PaymentTermType;
import com.unionsg.xaccounting.enums.Title;
import com.unionsg.xaccounting.enums.CustomerType;
import com.unionsg.xaccounting.enums.CustomerStatus;


public class SupplierMapper {

    public static Supplier toEntity(CreateSupplierRequestDTO dto){
        Address billing = Address.builder()
                .addressLine(dto.getBillingAddress().getAddressLine())
                .city(dto.getBillingAddress().getCity())
                .state(dto.getBillingAddress().getState())
                .zipCode(dto.getBillingAddress().getZipCode())
                .country(dto.getBillingAddress().getCountry())
                .addressType(AddressType.BILLING)
                .build();

        Address shipping  = Address.builder()
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

        return Supplier.builder()
                .supplierType(CustomerType.valueOf(dto.getSupplierType()))
                .title(dto.getTitle() != null ? Title.valueOf(dto.getTitle()) : null)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .companyName(dto.getCompanyName())
                .displayName(dto.getDisplayName())
                .status(CustomerStatus.valueOf(dto.getStatus())) // used customer status cos its going to be same for the suppler
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .mobile(dto.getMobile())
                .website(dto.getWebsite())
                .billingAddress((billing))
                .shippingAddress(shipping)
                .taxInfo(taxInfo)
                .paymentTerms(paymentTerms)
                .build();
    }

    public static SupplierResponseDTO toResponse(Supplier supplier) {
        return SupplierResponseDTO.builder()
                .id(supplier.getId())
                .supplierCode(supplier.getSupplierCode())
                .displayName(supplier.getDisplayName())
                .supplierType(supplier.getSupplierType().name())
                .status(supplier.getStatus().name())
                .email(supplier.getEmail())
                .build();
    }
}
