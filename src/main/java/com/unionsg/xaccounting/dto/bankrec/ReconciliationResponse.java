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
public class ReconciliationResponse {
    private Long id;
    private String reconciliationNumber;
    private Long bankAccountId;
    private String bankAccountName;
    private String bankName;
    private String bankAccountNumber;
    private String glAccountCode;
    private String currency;
    private LocalDate statementDate;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal openingBankBalance;
    private BigDecimal closingBankBalance;
    private BigDecimal bookBalance;
    private BigDecimal outstandingDeposits;
    private BigDecimal outstandingWithdrawals;
    private BigDecimal bankCharges;
    private BigDecimal bankInterest;
    private BigDecimal adjustmentsTotal;
    private BigDecimal difference;
    private BigDecimal tolerance;
    private Boolean withinTolerance;
    private ReconciliationStatus status;
    private String notes;
    private String preparedByName;
    private LocalDateTime preparedAt;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
    private String completedByName;
    private LocalDateTime completedAt;
    private Boolean differenceOverridden;
    private String overrideReason;
    private String reopenedByName;
    private LocalDateTime reopenedAt;
    private String reopenReason;
    private Integer reopenCount;
    private String cancelledByName;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private LocalDateTime lastAutoMatchAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
