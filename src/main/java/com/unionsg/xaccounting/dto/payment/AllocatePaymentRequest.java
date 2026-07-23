package com.unionsg.xaccounting.dto.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AllocatePaymentRequest {

    @NotEmpty(message = "At least one allocation is required")
    @Valid
    private List<PaymentAllocationRequest> allocations;
}

