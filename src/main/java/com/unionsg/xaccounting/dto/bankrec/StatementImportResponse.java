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


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementImportResponse {
    private Long id;
    private Long bankAccountId;
    private String bankAccountName;
    private Long reconciliationId;
    private String reconciliationNumber;
    private Long profileId;
    private String profileName;
    private String fileName;
    private LocalDate firstTransactionDate;
    private LocalDate lastTransactionDate;
    private Integer totalRows;
    private Integer importedRows;
    private Integer duplicateRows;
    private Integer errorRows;
    private String importedByName;
    private LocalDateTime importedAt;
    private Boolean dryRun;
    private List<ImportRowResponse> rows;
}
