package com.unionsg.xaccounting.dto.supplier;

import lombok.Data;

@Data
public class SupplierTaxInfoDTO {
    private String taxId;
    private Boolean withholding;
    private double rate;
}
