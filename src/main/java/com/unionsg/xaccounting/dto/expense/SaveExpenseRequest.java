package com.unionsg.xaccounting.dto.expense;

import com.unionsg.xaccounting.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Create or update a draft expense. Amounts are in the payment account's currency;
 * {@code exchangeRate} (1 unit of it in base currency) is only needed when that currency is
 * not the base currency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveExpenseRequest {

    private Long supplierId;

    @NotNull(message = "Payment account is required")
    private Long paymentAccountId;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @DecimalMin(value = "0.0000000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRate;

    @Size(max = 100, message = "Reference can be at most 100 characters")
    private String reference;

    @Size(max = 1000, message = "Memo can be at most 1000 characters")
    private String memo;

    @NotEmpty(message = "Add at least one expense line")
    @Size(max = 200, message = "An expense can have at most 200 lines")
    @Valid
    private List<SaveExpenseLineRequest> lines;

    /** The version the client loaded, for updates; a stale value is refused. */
    private Long version;
}
