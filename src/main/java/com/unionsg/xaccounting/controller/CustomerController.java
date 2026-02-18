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
        CustomerResponseDTO response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<CustomerResponseDTO>builder()
                        .success(true)
                        .message("Customer created successfully")
                        .data(response)
                        .build()
                );
    }

    @GetMapping
    public ResponseEntity<PaginationResponse<CustomerResponseDTO>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
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
                        .data(response)
                        .build()
        );
    }
}
