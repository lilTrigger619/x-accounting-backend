package com.unionsg.xaccounting.service.customer;

import com.unionsg.xaccounting.dto.customer.CustomerResponseDTO;
import com.unionsg.xaccounting.dto.customer.CreateCustomerRequestDTO;
import com.unionsg.xaccounting.response.PaginationResponse;


public interface CustomerService {
    CustomerResponseDTO createCustomer(CreateCustomerRequestDTO request);

    CustomerResponseDTO getCustomer(Long id);

    PaginationResponse<CustomerResponseDTO> getAllCustomers(int page, int size, String sortBy, String sortDir, String search);
};
