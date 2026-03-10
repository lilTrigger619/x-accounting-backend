package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.customer.*;
import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.response.PaginationResponse;
import com.unionsg.xaccounting.service.customer.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> createCustomer(@Valid @RequestBody CreateCustomerRequestDTO request ){
        System.out.println("hello from the con");
        CustomerResponseDTO response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CustomerResponseDTO>builder()
                        .success(true)
                        .message("Customer created successfully")
                        .content(response)
                        .build()
                );
    }

    @GetMapping
    public ResponseEntity<PaginationResponse<CustomerResponseDTO>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search
    ){
        PaginationResponse<CustomerResponseDTO> response =
                customerService.getAllCustomers(page, size, sortBy, sortDir, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponseDTO>> getCustomer(@PathVariable Long id){
        CustomerResponseDTO response = customerService.getCustomer(id);

        return ResponseEntity.ok(
                ApiResponse.<CustomerResponseDTO>builder()
                        .success(true)
                        .message("Customer retrieved successfully")
                        .content(response)
                        .build()
        );
    }

    @GetMapping("/{id}/getPaymentTerm")
    public ResponseEntity<ApiResponse<PaymentTermsDTO>> getCustomerPaymentTerm(@PathVariable Long id){
        PaymentTermsDTO paymentTerm = customerService.getCustomerPaymentTerms(id);
        return ResponseEntity.ok(
                ApiResponse.<PaymentTermsDTO>builder()
                        .success(true)
                        .message("Payment terms retrieved")
                        .content(paymentTerm)
                        .build()
        );
    }

    @GetMapping("/{id}/getBillingAddress")
    public ResponseEntity<ApiResponse<AddressDTO>> getBillingAddress(@PathVariable Long id){
        AddressDTO customerAddress = customerService.getCustomerBillingAddress(id);
        return ResponseEntity.ok(
                ApiResponse.<AddressDTO>builder()
                        .success(true)
                        .message("Billing address retrieved")
                        .content(customerAddress)
                        .build()
        );
    }
}
