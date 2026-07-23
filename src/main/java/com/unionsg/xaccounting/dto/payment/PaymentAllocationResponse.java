package com.unionsg.xaccounting.dto.payment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentAllocationResponse {

    private Long allocationId;

    private Long invoiceId;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    private BigDecimal invoiceTotal;

    private BigDecimal outstandingBefore;

    private BigDecimal allocatedAmount;

    private BigDecimal outstandingAfter;
}

