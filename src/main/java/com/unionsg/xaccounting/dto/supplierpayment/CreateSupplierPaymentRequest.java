package com.unionsg.xaccounting.dto.supplierpayment;

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
public class CreateSupplierPaymentRequest {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private Long bankAccountId;

    @NotNull(message = "Currency is required")
    private Currency currency;

    @PositiveOrZero(message = "Exchange rate must be zero or positive")
    private BigDecimal exchangeRate;

    @NotNull(message = "Amount paid is required")
    @Positive(message = "Amount paid must be positive")
    private BigDecimal amountPaid;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;

    @Size(max = 500, message = "Memo must not exceed 500 characters")
    private String memo;

    private List<Long> attachmentIds;

    private List<SupplierPaymentAllocationRequest> allocations;
}
