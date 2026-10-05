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


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveReconciliationRequest {
    private Long bankAccountId;
    private LocalDate statementDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    /** Defaults to the closing balance of the account's last completed reconciliation. */
    private BigDecimal openingBankBalance;
    private BigDecimal closingBankBalance;
    private BigDecimal tolerance;
    private String notes;
}
