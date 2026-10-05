package com.unionsg.xaccounting.dto.banking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/** What a transfer form's values would post, without saving anything. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferPreviewResponse {
    private String baseCurrency;
    private String sourceCurrency;
    private String destinationCurrency;
    private BigDecimal exchangeRate;
    private BigDecimal convertedAmount;
    private BigDecimal sourceBaseRate;
    private BigDecimal destinationBaseRate;
    private BigDecimal baseAmount;
    private BigDecimal baseConvertedAmount;
    private BigDecimal baseFeeAmount;
    private BigDecimal exchangeGainLoss;
    private List<BankTransferAccountingLine> lines;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal sourceAvailableBalance;
    private Boolean sufficientBalance;
    private Boolean sourceAllowsOverdraft;
}
