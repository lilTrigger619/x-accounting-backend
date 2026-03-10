package com.unionsg.xaccounting.controller;

import com.unionsg.xaccounting.dto.supplier.*;
import com.unionsg.xaccounting.response.ApiResponse;
import com.unionsg.xaccounting.response.PaginationResponse;
import com.unionsg.xaccounting.service.supplier.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierService supplierService;

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> createSupplier(@Valid @RequestBody CreateSupplierRequestDTO request) {
       SupplierResponseDTO response = supplierService.createSupplier(request);
       return ResponseEntity.status(HttpStatus.CREATED)
               .body(ApiResponse.<SupplierResponseDTO>builder()
                       .success(true)
                       .message("Supplier created successfully")
                       .content(response)
                       .build()
               );
    };

    @GetMapping
    public ResponseEntity<PaginationResponse<SupplierResponseDTO>> getCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "displayName")  String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search
            ){
        PaginationResponse<SupplierResponseDTO> response = supplierService.getAllSuppliers(page, size, sortBy, sortDir, search);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> getSupplier(@PathVariable Long id){
        SupplierResponseDTO response = supplierService.getSupplier(id);

        return ResponseEntity.ok(
                ApiResponse.<SupplierResponseDTO>builder()
                        .success(true)
                        .message("Supplier created successfully")
                        .content(response)
                        .build()
        );
    }
}
