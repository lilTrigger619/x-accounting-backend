package com.unionsg.xaccounting.entity.fixedasset;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.enums.fixedasset.DepreciationRunStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Depreciation for one accounting period, from calculation through approval to posting.
 *
 * <p>{@code activePeriodId} holds the period's id while the run is live (anything but cancelled or
 * reversed) and is unique, so the database itself refuses a second live run for the same period no
 * matter how often a user repeats the operation.</p>
 */
@Entity
@Table(name = "depreciation_runs")
@Getter
@Setter
public class DepreciationRun extends BaseEntity {

    @Column(name = "run_number", nullable = false, unique = true, length = 50)
    private String runNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriod accountingPeriod;

    @Column(name = "active_period_id", unique = true)
    private Long activePeriodId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    /** The journal date: the period end. */
    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepreciationRunStatus status = DepreciationRunStatus.CALCULATED;

    @Column(name = "total_depreciation", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDepreciation = BigDecimal.ZERO;

    @Column(name = "asset_count", nullable = false)
    private Integer assetCount = 0;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0;

    @Column(name = "skipped_count", nullable = false)
    private Integer skippedCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_journal_id")
    private JournalEntry reversalJournal;

    @Column(length = 1000)
    private String notes;

    @Column(name = "status_reason", length = 1000)
    private String statusReason;

    private LocalDateTime calculatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculated_by")
    private User calculatedBy;

    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by")
    private User submittedBy;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime postedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by")
    private User postedBy;

    private LocalDateTime cancelledAt;

    private LocalDateTime reversedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DepreciationRunLine> lines = new ArrayList<>();
}
