package com.unionsg.xaccounting.dto.supplierpayment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SupplierPaymentAllocationResponse {

    private Long allocationId;

    private Long billId;

    private String billNumber;

    private LocalDate billDate;

    private BigDecimal billTotal;

    private BigDecimal outstandingBefore;

    private BigDecimal allocatedAmount;

    private BigDecimal outstandingAfter;
}
