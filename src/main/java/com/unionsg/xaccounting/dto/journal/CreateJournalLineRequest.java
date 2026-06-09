package com.unionsg.xaccounting.dto.journal;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateJournalLineRequest {
    @NotNull(message = "Account ID is required")
    private Long accountId;

    @Size(max = 500)
    private String description;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal debitAmount;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal creditAmount;
}
