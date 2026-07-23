package com.unionsg.xaccounting.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentAllocationRequest {

    @NotNull(message = "Invoice ID is required")
    private Long invoiceId;

    @NotNull(message = "Allocated amount is required")
    @Positive(message = "Allocated amount must be positive")
    private BigDecimal allocatedAmount;
}

