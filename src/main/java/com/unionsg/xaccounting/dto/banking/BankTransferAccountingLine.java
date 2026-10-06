package com.unionsg.xaccounting.dto.banking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** One debit or credit of a transfer's accounting impact, in base currency. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferAccountingLine {
    private String accountCode;
    private String accountName;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
}
