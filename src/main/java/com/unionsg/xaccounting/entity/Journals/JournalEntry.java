package com.unionsg.xaccounting.entity.Journals;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.branch.Branch;
import com.unionsg.xaccounting.entity.branch.CostCenter;
import com.unionsg.xaccounting.enums.JournalStatus;
import com.unionsg.xaccounting.enums.JournalType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
//@Table(name="journal_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "journal_entries",
        indexes = {
                @Index(name = "idx_journal_entry_number", columnList = "journal_number"),
                @Index(name = "idx_journal_entry_date", columnList = "journal_date"),
                @Index(name = "idx_journal_entry_status", columnList = "status"),
                @Index(name = "idx_journal_entry_branch", columnList = "branch_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_journal_number", columnNames = "journal_number")
        }
)
public class JournalEntry extends BaseEntity {

    @Column(name = "journal_number", nullable = false, length = 50)
    private String journalNumber;

    @Column(name = "reference", length = 100, unique = true)
    private String reference;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "journal_type", nullable = false, length = 30)
    private JournalType journalType = JournalType.GENERAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JournalStatus status = JournalStatus.DRAFT;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(name = "posting_date")
    private LocalDate postingDate;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Builder.Default
    @Column(name = "is_system_generated", nullable = false)
    private Boolean systemGenerated = false;

    @Builder.Default
    @Column(name = "is_adjustment", nullable = false)
    private Boolean adjustmentEntry = false;

    @Builder.Default
    @Column(name = "currency_code", nullable = false, length = 10)
    private String currencyCode = "GHS";

    @Builder.Default
    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Builder.Default
    @Column(name = "total_debit", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_credit", precision = 19, scale = 2, nullable = false)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "source_module", length = 50)
    private String sourceModule;

    @Column(name = "source_entity_id")
    private Long sourceEntityId;

    @Column(name = "reversal_of_journal_id")
    private Long reversalOfJournalId;

    /** Originating branch; the hub that inter-branch balancing lines pair against. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    /** Default cost center for lines that don't name their own. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_center_id")
    private CostCenter costCenter;

    /** Entered by hand on the Journal screen (as opposed to posted by a module). */
    @Builder.Default
    @Column(name = "is_manual_entry", nullable = false, columnDefinition = "boolean default false not null")
    private Boolean manualEntry = false;

    // ===== Approval =====

    @Builder.Default
    @Column(name = "requires_approval", nullable = false, columnDefinition = "boolean default false not null")
    private Boolean requiresApproval = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Builder.Default
    @OneToMany(
            mappedBy = "journalEntry",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("lineNumber ASC")
    private List<JournalLine> lines = new ArrayList<>();

    // ===== Convenience Methods =====

    public void addLine(JournalLine line) {
        line.setJournalEntry(this);
        this.lines.add(line);
    }

    public void removeLine(JournalLine line) {
        line.setJournalEntry(null);
        this.lines.remove(line);
    }


}
