package com.unionsg.xaccounting.dto.payment;

import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CreatePaymentRequest {

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private Long bankAccountId;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @PositiveOrZero(message = "Exchange rate must be zero or positive")
    private BigDecimal exchangeRate;

    @NotNull(message = "Amount received is required")
    @Positive(message = "Amount received must be positive")
    private BigDecimal amountReceived;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    @Size(max = 500, message = "Memo must not exceed 500 characters")
    private String memo;

    private List<Long> attachmentIds;

    private List<PaymentAllocationRequest> allocations;
}

