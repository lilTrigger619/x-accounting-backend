package com.unionsg.xaccounting.dto.journal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.unionsg.xaccounting.dto.CreatedByDTO;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JournalResponse {

    private Long id;

    private String journalNumber;

    private String reference;

    private String description;

    private JournalType journalType;

    private JournalStatus status;

    private LocalDate journalDate;

    private LocalDate postingDate;

    private BigDecimal totalDebit;

    private BigDecimal totalCredit;

    private String currencyCode;

    private Boolean systemGenerated;

    private Boolean adjustmentEntry;

    private String sourceModule;

    private Long sourceEntityId;

    private LocalDateTime postedAt;

    private LocalDateTime reversedAt;

    private LocalDateTime createdAt;

    private CreatedByDTO createdBy;

    private List<JournalLineResponse> lines;

    /** Set on a reversing entry: the journal it reverses. */
    private Long reversalOfJournalId;

    /** Set on a reversed journal (single-journal reads only): the entry that reversed it. */
    private JournalReversalResponse reversal;

    private Long branchId;
    private String branchCode;
    private String branchName;
    private Long costCenterId;
    private String costCenterCode;
    private String costCenterName;

    private Boolean manualEntry;
    private Boolean requiresApproval;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectedByName;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    /** True when every branch's lines balance on their own. */
    private Boolean balancedPerBranch;
    /** What the current user may do next with this journal. */
    private Boolean canSubmit;
    private Boolean canApprove;
    private Boolean canPost;
}
