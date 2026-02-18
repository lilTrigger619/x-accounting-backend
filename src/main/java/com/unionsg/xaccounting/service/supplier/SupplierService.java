package com.unionsg.xaccounting.service.supplier;

import com.unionsg.xaccounting.dto.customer.CustomerResponseDTO;
import com.unionsg.xaccounting.dto.supplier.SupplierResponseDTO;
import com.unionsg.xaccounting.dto.supplier.CreateSupplierRequestDTO;
import com.unionsg.xaccounting.response.PaginationResponse;

public interface SupplierService {
    SupplierResponseDTO createSupplier(CreateSupplierRequestDTO request);

    SupplierResponseDTO getSupplier(Long id);

    PaginationResponse<SupplierResponseDTO> getAllSuppliers(int page, int size, String sortBy, String sortDir, String search);
};
