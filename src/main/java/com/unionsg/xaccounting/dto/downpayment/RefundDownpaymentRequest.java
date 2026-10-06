package com.unionsg.xaccounting.dto.downpayment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RefundDownpaymentRequest {
    private BigDecimal amount;
    private LocalDate refundDate;
    /** Bank/cash account the refund is paid from (customer) or received into (supplier); defaults to the downpayment's. */
    private Long bankAccountId;
    private String reference;
    private String reason;
}
