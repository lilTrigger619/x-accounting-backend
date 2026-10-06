package com.unionsg.xaccounting.dto.expense;

import com.unionsg.xaccounting.dto.CreatedByDTO;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.expense.ExpenseStatus;
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
public class ExpenseListItemResponse {
    private Long id;
    private String expenseNumber;
    private String reference;
    private LocalDate paymentDate;
    private Long supplierId;
    private String supplierName;
    private ExpensePaymentAccount paymentAccount;
    private PaymentMethod paymentMethod;
    private String currency;
    private BigDecimal totalAmount;
    private int lineCount;
    private ExpenseStatus status;
    private CreatedByDTO createdBy;
    private LocalDateTime createdAt;
}
