package com.unionsg.xaccounting.dto.payment;

import com.unionsg.xaccounting.enums.RefundStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentRefundResponse {

    private Long refundId;

    private LocalDate refundDate;

    private BigDecimal amount;

    private String reason;

    private String referenceNumber;

    private RefundStatus status;
}

