package com.unionsg.xaccounting.dto.customer;

import lombok.Data;

@Data
public class TaxInfoDTO {
    private String taxId;
    private Boolean taxExempt;
    private String taxExemptReason;
}
