package com.unionsg.xaccounting.dto.downpayment;

import lombok.Data;

import java.math.BigDecimal;

/** How one customer's/supplier's downpayment balance moved over a period. */
@Data
public class DownpaymentMovementRow {
    private Long counterpartyId;
    private String counterpartyName;
    private String currency;
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal received = BigDecimal.ZERO;
    /** Net of applications reversed in the period. */
    private BigDecimal applied = BigDecimal.ZERO;
    /** Net of refunds reversed in the period. */
    private BigDecimal refunded = BigDecimal.ZERO;
    /** Downpayments whose receipt/payment was reversed in the period. */
    private BigDecimal reversed = BigDecimal.ZERO;
    private BigDecimal closingBalance = BigDecimal.ZERO;
}
