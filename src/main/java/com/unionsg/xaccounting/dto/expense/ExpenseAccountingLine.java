package com.unionsg.xaccounting.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** One debit or credit of an expense's journal, posted or previewed, in base currency. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAccountingLine {
    private String accountCode;
    private String accountName;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
}
