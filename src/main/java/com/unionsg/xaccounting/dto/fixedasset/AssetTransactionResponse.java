package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.AssetTransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetTransactionResponse {
    private Long id;
    private AssetTransactionType type;
    private AssetTransactionType movementType;
    private LocalDate transactionDate;
    private BigDecimal costAmount;
    private BigDecimal depreciationAmount;
    private BigDecimal impairmentAmount;
    private BigDecimal proceedsAmount;
    private BigDecimal gainLossAmount;
    private Long journalId;
    private String journalNumber;
    private Long runId;
    private String runNumber;
    private Long reversesId;
    private Boolean reversed;
    private Boolean reversible;
    private String description;
    private String createdBy;
    private LocalDateTime createdAt;
}
