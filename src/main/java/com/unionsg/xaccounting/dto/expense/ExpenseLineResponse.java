package com.unionsg.xaccounting.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseLineResponse {
    private Long id;
    private Integer lineNumber;
    private LocalDate expenseDate;
    private String category;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String description;
    private BigDecimal amount;
    private BigDecimal baseAmount;
}
