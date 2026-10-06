package com.unionsg.xaccounting.dto.expense;

import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.expense.ExpenseStatus;
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
public class ExpenseFilter {
    /** Matches expense number, reference, memo or supplier name. */
    private String search;
    private ExpenseStatus status;
    private Long supplierId;
    private Long paymentAccountId;
    private PaymentMethod paymentMethod;
    private Long accountId;
    private String category;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private UUID createdById;
}
