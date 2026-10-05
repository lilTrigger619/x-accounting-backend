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
import com.unionsg.xaccounting.enums.bankrec.StatementTransactionStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementTransactionResponse {
    private Long id;
    private Long bankAccountId;
    private Long statementImportId;
    private Integer sourceRowNumber;
    private LocalDate transactionDate;
    private LocalDate valueDate;
    private String description;
    private String reference;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal amount;
    private BigDecimal runningBalance;
    private String externalTransactionId;
    private BigDecimal matchedAmount;
    private BigDecimal remainingAmount;
    private StatementTransactionStatus status;
    private Long clearedInReconciliationId;
    private LocalDateTime importedAt;
}
