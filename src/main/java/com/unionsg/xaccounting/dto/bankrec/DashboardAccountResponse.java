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
import com.unionsg.xaccounting.enums.bankrec.ReconciliationStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAccountResponse {
    private Long bankAccountId;
    private String bankAccountName;
    private String bankName;
    private String accountNumber;
    private String glAccountCode;
    private String currency;
    private BigDecimal currentBookBalance;
    private LocalDate lastReconciledPeriodEnd;
    private BigDecimal lastReconciledClosingBalance;
    private Long openReconciliationId;
    private String openReconciliationNumber;
    private ReconciliationStatus openReconciliationStatus;
    private BigDecimal openReconciliationDifference;
    private Long unmatchedStatementCount;
    private Long daysSinceLastReconciliation;
}
