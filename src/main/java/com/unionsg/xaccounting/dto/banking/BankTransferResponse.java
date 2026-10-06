package com.unionsg.xaccounting.dto.banking;

import com.unionsg.xaccounting.dto.CreatedByDTO;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferResponse {
    private Long id;
    private String transferNumber;
    private String reference;
    private String description;
    private BankTransferStatus status;

    private BankTransferAccountSummary sourceAccount;
    private BankTransferAccountSummary destinationAccount;
    private LocalDate transferDate;
    private LocalDate valueDate;

    private BigDecimal amount;
    private String sourceCurrency;
    private String destinationCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private BigDecimal feeAmount;
    private String feeAccountCode;
    private String feeAccountName;

    private String baseCurrency;
    private BigDecimal sourceBaseRate;
    private BigDecimal destinationBaseRate;
    private BigDecimal baseAmount;
    private BigDecimal baseConvertedAmount;
    private BigDecimal baseFeeAmount;
    private BigDecimal exchangeGainLoss;

    /** Posted journal lines once posted; otherwise what posting would generate. */
    private List<BankTransferAccountingLine> accountingLines;
    /** Why the preview could not be built (e.g. a missing account mapping), for drafts. */
    private String accountingPreviewError;

    private Long journalId;
    private String journalNumber;
    private Long reversalJournalId;
    private String reversalJournalNumber;

    private CreatedByDTO createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime postedAt;
    private String postedBy;
    private LocalDateTime reversedAt;
    private String reversedBy;
    private String reversalReason;
    private LocalDateTime cancelledAt;
    private String cancelledBy;
    private String cancellationReason;

    private Long version;
}
