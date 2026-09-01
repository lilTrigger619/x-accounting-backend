package com.unionsg.xaccounting.dto.supplierpayment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AllocateSupplierPaymentRequest {

    @NotEmpty(message = "At least one allocation is required")
    @Valid
    private List<SupplierPaymentAllocationRequest> allocations;
}
