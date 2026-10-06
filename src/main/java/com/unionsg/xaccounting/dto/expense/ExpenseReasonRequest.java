package com.unionsg.xaccounting.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseReasonRequest {

    @NotBlank(message = "A reason is required to reverse an expense")
    @Size(max = 500, message = "Reason can be at most 500 characters")
    private String reason;
}
