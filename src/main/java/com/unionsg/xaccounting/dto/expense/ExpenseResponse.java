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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private Long id;
    private String expenseNumber;
    private String reference;
    private ExpenseStatus status;
    private Long supplierId;
    private String supplierName;
    private String supplierCode;
    private ExpensePaymentAccount paymentAccount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String currency;
    private String baseCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal totalAmount;
    private BigDecimal baseTotalAmount;
    private String memo;
    private List<ExpenseLineResponse> lines;
    /** The posted journal's lines, or for a draft what posting would create. */
    private List<ExpenseAccountingLine> accountingLines;
    /** Why a draft's journal can't be built yet (e.g. a missing account); null when it can. */
    private String accountingPreviewError;
    private Long journalId;
    private String journalNumber;
    private Long reversalJournalId;
    private String reversalJournalNumber;
    private CreatedByDTO createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private LocalDateTime postedAt;
    private String postedBy;
    private LocalDateTime reversedAt;
    private String reversedBy;
    private String reversalReason;
    private Long version;
}
