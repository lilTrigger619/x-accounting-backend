package com.unionsg.xaccounting.dto.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RefundPaymentRequest {

    @NotNull(message = "Refund date is required")
    private LocalDate refundDate;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    @Size(max = 500, message = "Memo must not exceed 500 characters")
    private String memo;
}

