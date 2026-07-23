package com.unionsg.xaccounting.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PaymentSummaryResponse {

    private BigDecimal amountReceived;

    private BigDecimal allocatedAmount;

    private BigDecimal unallocatedAmount;

    private BigDecimal customerCreditAfterAllocation;
}

