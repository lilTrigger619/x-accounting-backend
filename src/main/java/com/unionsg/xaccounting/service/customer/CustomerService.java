package com.unionsg.xaccounting.service.customer;

import com.unionsg.xaccounting.dto.customer.CustomerResponseDTO;
import com.unionsg.xaccounting.dto.customer.CreateCustomerRequestDTO;
import com.unionsg.xaccounting.response.PaginationResponse;

import com.unionsg.xaccounting.dto.customer.PaymentTermsDTO;
import com.unionsg.xaccounting.dto.customer.PaymentTermsRequestDTO;
import com.unionsg.xaccounting.dto.customer.AddressDTO;

public interface CustomerService {
    CustomerResponseDTO createCustomer(CreateCustomerRequestDTO request);

    CustomerResponseDTO getCustomer(Long id);

    PaymentTermsDTO getCustomerPaymentTerms(Long id);

    AddressDTO getCustomerBillingAddress(Long customerId);

    CustomerResponseDTO updateStatus(Long id, String status);

    PaginationResponse<CustomerResponseDTO> getAllCustomers(int page, int size, String sortBy, String sortDir, String search);
};
