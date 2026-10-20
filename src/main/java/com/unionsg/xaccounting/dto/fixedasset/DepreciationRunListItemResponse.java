package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepreciationRunListItemResponse {
    private Long id;
    private String runNumber;
    private String periodName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private LocalDate postingDate;
    private DepreciationRunStatus status;
    private BigDecimal totalDepreciation;
    private Integer assetCount;
    private Integer errorCount;
    private Long journalId;
    private String journalNumber;
    private Long reversalJournalId;
    private String reversalJournalNumber;
    private String postedBy;
    private LocalDateTime postedAt;
    private LocalDateTime createdAt;
}
