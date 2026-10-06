package com.unionsg.xaccounting.dto.expense;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveExpenseLineRequest {

    /** Defaults to the payment date when empty. */
    private LocalDate expenseDate;

    /** Code of an "expense-categories" configuration item. */
    @NotBlank(message = "Choose a category for every line")
    @Size(max = 100)
    private String category;

    /** Database id of the expense account the line is debited to. */
    @NotNull(message = "Choose an expense account for every line")
    private Long accountId;

    @NotBlank(message = "Every line needs a description")
    @Size(max = 500, message = "A line description can be at most 500 characters")
    private String description;

    @NotNull(message = "Every line needs an amount")
    @DecimalMin(value = "0.01", message = "Line amounts must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Line amounts can have at most 2 decimal places")
    private BigDecimal amount;
}
