package com.unionsg.xaccounting.service.customer;

import com.unionsg.xaccounting.dto.customer.CustomerResponseDTO;
import com.unionsg.xaccounting.dto.customer.CreateCustomerRequestDTO;


public interface CustomerService {
    CustomerResponseDTO createCustomer(CreateCustomerRequestDTO request);

    CustomerResponseDTO getCustomer(Long id);
}
