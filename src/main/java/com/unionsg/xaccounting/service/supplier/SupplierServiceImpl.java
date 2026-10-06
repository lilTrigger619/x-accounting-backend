package com.unionsg.xaccounting.service.supplier;

import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.dto.supplier.*;
import com.unionsg.xaccounting.MapperLayer.SupplierMapper;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.response.PaginationResponse;
import com.unionsg.xaccounting.service.supplier.SupplierService;

import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService{
    private final SupplierRepository supplierRepository;
    private final ConfigValueValidator configValues;

    @Override
    public SupplierResponseDTO createSupplier(CreateSupplierRequestDTO request){
        // basic validation
        if (supplierRepository.existsByDisplayName(request.getDisplayName())){
            throw new IllegalArgumentException("Display name already Exists");
        }

        Supplier supplier = SupplierMapper.toEntity(request);
        supplier.setCategory(configValues.require("supplier-categories", supplier.getCategory(), null, "Category"));
        supplier.getPaymentTerms().setCurrency(
                configValues.require("currencies", supplier.getPaymentTerms().getCurrency(), null, "Currency"));
        String method = supplier.getPaymentTerms().getPaymentMethod();
        if (method != null && !method.isBlank()) {
            try {
                supplier.getPaymentTerms().setPaymentMethod(PaymentMethod.valueOf(method.trim()).name());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("\"" + method + "\" is not a valid payment method");
            }
        } else {
            supplier.getPaymentTerms().setPaymentMethod(null);
        }
        if (supplier.getAddress() != null) {
            supplier.getAddress().setCountry(
                    configValues.validate("countries", supplier.getAddress().getCountry(), null, "Country"));
        }
        supplier.setSupplierCode(generateSupplierCode());

        Supplier saved = supplierRepository.save(supplier);
        return SupplierMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplier(Long id){
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(()->new RuntimeException("Customer not found"));
        return SupplierMapper.toResponse(supplier);
    }

    @Override
    public PaginationResponse<SupplierResponseDTO> getAllSuppliers(int page, int size, String sortBy, String sortDir, String search){
        Sort sort =
                sortDir.equalsIgnoreCase("desc") ?
                        Sort.by(sortBy).descending():
                        Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page <Supplier> supplierPage;

        if (search != null && !search.isEmpty()){
            supplierPage = supplierRepository.findAll((root, query, cb)->

                           cb.or(
                                   cb.like(cb.lower(root.get("displayName")), "%"+search.toLowerCase() + "%"),
                                   cb.like(cb.lower(root.get("email")), "%"+ search.toLowerCase()+ "%"),
                                   cb.like(cb.lower(root.get("companyName")), "%"+search.toLowerCase()+"%"),
                                   cb.like(cb.lower(root.get("contactPerson")), "%"+search.toLowerCase()+"%")
                           ), pageable);
        }else {
            supplierPage = supplierRepository.findAll(pageable);
        }

        List<SupplierResponseDTO> dtos = supplierPage.getContent()
                .stream()
                .map(SupplierMapper::toResponse)
                .toList();

        return PaginationResponse.<SupplierResponseDTO>builder()
                .content(dtos)
                .page(supplierPage.getNumber())
                .size(supplierPage.getSize())
                .totalElements(supplierPage.getTotalElements())
                .totalPages(supplierPage.getTotalPages())
                .last(supplierPage.isLast())
                .build();
    };

    private String generateSupplierCode(){
        return "SUPL-"+UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

