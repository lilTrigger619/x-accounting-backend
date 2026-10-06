package com.unionsg.xaccounting.dto.bankrec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAdjustmentType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdjustmentRequest {
    private ReconciliationAdjustmentType adjustmentType;
    /** The statement line being brought into the books; it is matched to the new journal. */
    private Long statementTransactionId;
    private LocalDate transactionDate;
    /** Positive; defaults to the statement line's unmatched amount. */
    private BigDecimal amount;
    /** AccountEntity id of the offset account; defaults to the type's mapped account. */
    private Long offsetAccountId;
    private String description;
    private String reference;
}
