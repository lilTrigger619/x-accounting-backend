package com.unionsg.xaccounting.dto.banking;

import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferFilter {
    /** Matches transfer number, reference or description. */
    private String search;
    private String transferNumber;
    private String reference;
    private Long sourceBankAccountId;
    private Long destinationBankAccountId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BankTransferStatus status;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private UUID createdById;
}
