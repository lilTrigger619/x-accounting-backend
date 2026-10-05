package com.unionsg.xaccounting.dto.banking;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Create or update a draft bank transfer. {@code amount} and {@code feeAmount} are in the
 * source account's currency. The rates are only needed when currencies differ; see
 * {@code BankTransferCalculator} for which ones are required and how missing ones default.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveBankTransferRequest {

    @NotNull(message = "Source account is required")
    private Long sourceBankAccountId;

    @NotNull(message = "Destination account is required")
    private Long destinationBankAccountId;

    @NotNull(message = "Transfer date is required")
    private LocalDate transferDate;

    private LocalDate valueDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount can have at most 2 decimal places")
    private BigDecimal amount;

    @DecimalMin(value = "0.000001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRate;

    @DecimalMin(value = "0.0000000001", message = "Source currency rate must be greater than zero")
    private BigDecimal sourceBaseRate;

    @DecimalMin(value = "0.0000000001", message = "Destination currency rate must be greater than zero")
    private BigDecimal destinationBaseRate;

    @DecimalMin(value = "0.00", message = "Transfer charges cannot be negative")
    @Digits(integer = 17, fraction = 2, message = "Transfer charges can have at most 2 decimal places")
    private BigDecimal feeAmount;

    /** Chart of Accounts code for the charges; defaults to the Bank Charges mapping. */
    @Size(max = 20)
    private String feeAccountCode;

    @Size(max = 100, message = "Reference can be at most 100 characters")
    private String reference;

    @Size(max = 500, message = "Description can be at most 500 characters")
    private String description;

    /** The version the client loaded, for updates; a stale value is refused. */
    private Long version;
}
