package com.unionsg.xaccounting.dto.supplier;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class SupplierResponseDTO {
    private Long id;
    private String supplierCode;
    private String supplierType;
    private String displayName;
    private String status;
    private String email;
}
