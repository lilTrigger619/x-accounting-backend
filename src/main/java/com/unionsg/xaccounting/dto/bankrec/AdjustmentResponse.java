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
import com.unionsg.xaccounting.enums.bankrec.AdjustmentStatus;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAdjustmentType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentResponse {
    private Long id;
    private Long reconciliationId;
    private ReconciliationAdjustmentType adjustmentType;
    private String adjustmentTypeLabel;
    private Boolean moneyIn;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private BigDecimal signedAmount;
    private String description;
    private String reference;
    private Long offsetAccountId;
    private String offsetAccountCode;
    private String offsetAccountName;
    private Long statementTransactionId;
    private Long journalId;
    private String journalNumber;
    private Long reversalJournalId;
    private AdjustmentStatus status;
    private String postedByName;
    private LocalDateTime postedAt;
    private String reversedByName;
    private LocalDateTime reversedAt;
    private String reversalReason;
}
