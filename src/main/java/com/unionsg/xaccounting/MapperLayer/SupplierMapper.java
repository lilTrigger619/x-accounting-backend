package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.supplier.CreateSupplierRequestDTO;
import com.unionsg.xaccounting.dto.supplier.SupplierResponseDTO;
import com.unionsg.xaccounting.entity.customer.Address;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
//import com.unionsg.xaccounting.entity.customer.TaxInfo;
import com.unionsg.xaccounting.entity.supplier.WithholdingTax;
import com.unionsg.xaccounting.entity.supplier.SupplierPaymentTerms;
import com.unionsg.xaccounting.enums.*;
import com.unionsg.xaccounting.entity.customer.Customer;


import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.Currency;


public class SupplierMapper {

    public static Supplier toEntity(CreateSupplierRequestDTO dto){
        Address address = Address.builder()
                .addressLine(dto.getAddress().getAddressLine())
                .city(dto.getAddress().getCity())
                .state(dto.getAddress().getState())
                .zipCode(dto.getAddress().getZipCode())
                .country(dto.getAddress().getCountry())
                .addressType(AddressType.BILLING)
                .build();


        WithholdingTax taxInfo = WithholdingTax.builder()
                .taxId(dto.getTaxInfo().getTaxId())
                .rate(dto.getTaxInfo().getRate())
                .withholding(dto.getTaxInfo().getWithholding())
                .build();

        SupplierPaymentTerms paymentTerms = SupplierPaymentTerms.builder()
                .paymentTermType(PaymentTermType.valueOf(dto.getPaymentTerms().getPaymentTermType()))
                .paymentMethod(dto.getPaymentTerms().getPaymentMethod())
                .currency(Currency.valueOf(dto.getPaymentTerms().getCurrency()))
                .build();

        return Supplier.builder()
                .supplierType(CustomerType.valueOf(dto.getSupplierType()))
//                .title(dto.getTitle() != null ? Title.valueOf(dto.getTitle()) : null)
//                .firstName(dto.getFirstName())
//                .lastName(dto.getLastName())
                .contactPerson(dto.getContactPerson())
                .companyName(dto.getCompanyName())
                .displayName(dto.getDisplayName())
                .status(CustomerStatus.valueOf(dto.getStatus())) // used customer status cos its going to be same for the suppler
                .category(SupplierCategory.valueOf(dto.getCategory()))
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .mobile(dto.getMobile())
                .website(dto.getWebsite())
                .address(address)
//                .shippingAddress(shipping)
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
