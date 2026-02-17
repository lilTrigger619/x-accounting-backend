package com.unionsg.xaccounting.service.customer;

import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.dto.customer.*;
import com.unionsg.xaccounting.MapperLayer.CustomerMapper;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService{
    private final CustomerRepository customerRepository;

    @Override
    public CustomerResponseDTO createCustomer(CreateCustomerRequestDTO request) {
       // Basic validation
        if (customerRepository.existsByDisplayName(request.getDisplayName())){
            throw new IllegalArgumentException("Display name already exists");
        }

        Customer customer = CustomerMapper.toEntity(request);
        customer.setCustomerCode(generateCustomerCode());

        Customer saved = customerRepository.save(customer);
        return CustomerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDTO getCustomer(Long id){
        Customer customer  = customerRepository.findById(id)
                .orElseThrow(()->new RuntimeException("Customer not found"));
        return CustomerMapper.toResponse(customer);
    }

    private String generateCustomerCode(){
        return "CUST-"+UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
