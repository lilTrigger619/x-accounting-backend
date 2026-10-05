package com.unionsg.xaccounting.dto.banking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** A bank account as shown on a transfer, and as offered by the transfer account picker. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferAccountSummary {
    private Long id;
    private String accountName;
    private String bankName;
    private String accountNumber;
    private String currency;
    private String glAccountCode;
    private String glAccountName;
    private String status;
    private Boolean isDefault;
    private Boolean allowOverdraft;
    /** Current GL balance in base currency; only filled by the picker endpoint. */
    private BigDecimal availableBalance;
}
