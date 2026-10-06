package com.unionsg.xaccounting.dto.deposit;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositTotalsResponse {
    private long count;
    private long openCount;
    private BigDecimal originalAmount = BigDecimal.ZERO;
    private BigDecimal appliedAmount = BigDecimal.ZERO;
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private BigDecimal forfeitedAmount = BigDecimal.ZERO;
    private BigDecimal transferredAmount = BigDecimal.ZERO;
    private BigDecimal availableBalance = BigDecimal.ZERO;
}
