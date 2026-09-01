package com.unionsg.xaccounting.dto.supplierpayment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SupplierPaymentAllocationRequest {

    @NotNull(message = "Bill ID is required")
    private Long billId;

    @NotNull(message = "Allocated amount is required")
    @Positive(message = "Allocated amount must be positive")
    private BigDecimal allocatedAmount;
}
