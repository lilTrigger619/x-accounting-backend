package com.unionsg.xaccounting.dto.accounting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecurringJournalTemplateLineResponse {
    private Long id;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private Integer lineNumber;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
}
