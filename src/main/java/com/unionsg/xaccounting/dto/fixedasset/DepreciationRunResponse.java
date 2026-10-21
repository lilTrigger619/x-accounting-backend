package com.unionsg.xaccounting.dto.fixedasset;

import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepreciationRunResponse {
    private Long id;
    private String runNumber;
    private Long accountingPeriodId;
    private String periodName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String periodStatus;
    private LocalDate postingDate;
    private DepreciationRunStatus status;
    private BigDecimal totalDepreciation;
    private Integer assetCount;
    private Integer skippedCount;
    private Integer errorCount;
    private Long journalId;
    private String journalNumber;
    private Long reversalJournalId;
    private String reversalJournalNumber;
    private String notes;
    private String statusReason;
    private LocalDateTime calculatedAt;
    private String calculatedBy;
    private LocalDateTime submittedAt;
    private String submittedBy;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime postedAt;
    private String postedBy;
    private LocalDateTime cancelledAt;
    private LocalDateTime reversedAt;
    private List<DepreciationRunLineResponse> lines;
    private List<JournalPreviewLine> journalPreview;
    private List<String> warnings;
}
