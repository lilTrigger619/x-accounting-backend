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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferListItemResponse {
    private Long id;
    private String transferNumber;
    private String reference;
    private LocalDate transferDate;
    private LocalDate valueDate;
    private BankTransferAccountSummary sourceAccount;
    private BankTransferAccountSummary destinationAccount;
    private BigDecimal amount;
    private String sourceCurrency;
    private BigDecimal convertedAmount;
    private String destinationCurrency;
    private BigDecimal feeAmount;
    private BankTransferStatus status;
    private CreatedByDTO createdBy;
    private LocalDateTime createdAt;
}
