package com.unionsg.xaccounting.dto.bankrec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.unionsg.xaccounting.enums.bankrec.AmountSignConvention;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementImportOptions {
    private Long bankAccountId;
    private Long reconciliationId;
    /** Saved mapping to start from; any mapping field below overrides it. */
    private Long profileId;
    /** Parse and report without saving anything. */
    private Boolean dryRun;
    private String delimiter;
    private Boolean hasHeaderRow;
    private Integer skipRows;
    private String dateFormat;
    private String transactionDateColumn;
    private String valueDateColumn;
    private String descriptionColumn;
    private String referenceColumn;
    private String debitColumn;
    private String creditColumn;
    private String amountColumn;
    private String balanceColumn;
    private String externalIdColumn;
    private AmountSignConvention amountSignConvention;
}
