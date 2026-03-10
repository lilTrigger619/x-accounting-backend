package com.unionsg.xaccounting.service.customer;

import com.unionsg.xaccounting.entity.customer.Address;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.customer.PaymentTerms;
import com.unionsg.xaccounting.dto.customer.*;
import com.unionsg.xaccounting.MapperLayer.CustomerMapper;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.response.PaginationResponse;
import com.unionsg.xaccounting.service.customer.CustomerService;
import lombok.RequiredArgsConstructor;
import org.hibernate.internal.build.AllowSysOut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.dto.customer.AddressDTO;

import com.unionsg.xaccounting.entity.customer.PaymentTerms;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService{
    private final CustomerRepository customerRepository;

    @Override
    public CustomerResponseDTO createCustomer(CreateCustomerRequestDTO request) {
       // Basic validation
        System.out.println("Hello");
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

    @Override
    public PaginationResponse<CustomerResponseDTO> getAllCustomers(int page, int size, String sortBy, String sortDir, String search){
       Sort sort =
               sortDir.equalsIgnoreCase("desc") ?
                       Sort.by(sortBy).descending() :
                       Sort.by(sortBy).ascending();
       Pageable pageable = PageRequest.of(page, size, sort);

       Page<Customer> customerPage;

       if (search != null && !search.isEmpty()){
           customerPage = customerRepository.findAll((root, query, cb) ->
                   cb.or(
                          cb.like(cb.lower(root.get("displayName")), "%" + search.toLowerCase() + "%"),
                           cb.like(cb.lower(root.get("email")), "%"+ search.toLowerCase() + "%"),
                           cb.like(cb.lower(root.get("phone")), "%"+ search.toLowerCase() + "%")
                   ), pageable);
       }else {
           customerPage = customerRepository.findAll(pageable);
       }

        List<CustomerResponseDTO> dtos = customerPage.getContent()
                .stream()
                .map(CustomerMapper::toResponse)
                .toList();

       return PaginationResponse.<CustomerResponseDTO>builder()
               .content(dtos)
               .page(customerPage.getNumber())
               .size(customerPage.getSize())
               .totalElements(customerPage.getTotalElements())
               .totalPages(customerPage.getTotalPages())
               .last(customerPage.isLast())
               .build();

    };

    @Override
    public PaymentTermsDTO getCustomerPaymentTerms(Long  id){
        Customer customer = customerRepository.findById(id)
                .orElseThrow(()-> new ResourceNotFoundException("Customer not found with provided id"));

        PaymentTerms paymentTerm = customer.getPaymentTerms();
        return PaymentTermsDTO.builder()
                .currency( paymentTerm.getCurrency().name())
                .creditLimit(paymentTerm.getCreditLimit())
                .paymentTermType(paymentTerm.getPaymentTermType().name())
                .build();
    }

    @Override
    public AddressDTO getCustomerBillingAddress(Long customerId){
       Customer customer = customerRepository.findById(customerId)
        .orElseThrow(()->new ResourceNotFoundException("No custoemr found with provided id"));
       Address customerAddress = customer.getBillingAddress();
       //PaymentTerms paymentTerm = customer.getPaymentTerms();
       return AddressDTO.builder()
               .addressLine(customerAddress.getAddressLine())
               .city(customerAddress.getCity())
               .zipCode(customerAddress.getZipCode())
               .country(customerAddress.getCountry())
               .build();
    }

    private String generateCustomerCode(){
        return "CUST-"+UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
