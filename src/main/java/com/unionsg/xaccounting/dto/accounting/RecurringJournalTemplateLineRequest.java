package com.unionsg.xaccounting.dto.accounting;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecurringJournalTemplateLineRequest {

    @NotNull(message = "Account is required")
    private Long accountId;

    @Size(max = 500)
    private String description;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal debitAmount;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal creditAmount;
}
