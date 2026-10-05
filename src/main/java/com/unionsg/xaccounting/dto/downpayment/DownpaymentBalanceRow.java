package com.unionsg.xaccounting.dto.downpayment;

import lombok.Data;

import java.math.BigDecimal;

/** Downpayment totals for one customer or supplier (liability / asset reports). */
@Data
public class DownpaymentBalanceRow {
    private Long counterpartyId;
    private String counterpartyName;
    private String currency;
    private long count;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal appliedAmount = BigDecimal.ZERO;
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    private BigDecimal availableBalance = BigDecimal.ZERO;
}
