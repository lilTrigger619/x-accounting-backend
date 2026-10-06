package com.unionsg.xaccounting.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** What the form's values would post, without saving anything. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpensePreviewResponse {
    private String currency;
    private String baseCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal totalAmount;
    private BigDecimal baseTotalAmount;
    private List<ExpenseAccountingLine> lines;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    /** The payment account's current GL balance, in base currency. */
    private BigDecimal availableBalance;
    private boolean sufficientBalance;
    private boolean allowsOverdraft;
}
