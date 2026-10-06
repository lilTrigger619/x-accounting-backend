package com.unionsg.xaccounting.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The bank or cash account an expense was paid from. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpensePaymentAccount {
    private Long id;
    private String accountName;
    private String bankName;
    private String accountNumber;
    private String currency;
    private String glAccountCode;
}
