package com.unionsg.xaccounting.entity.accounting;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.FinancialYearStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a controlled financial reporting calendar year, divided into AccountingPeriods.
 * Financial Years must not overlap; only one may be the current/active year at a time.
 */
@Entity
@Table(name = "financial_years")
@Getter
@Setter
public class FinancialYear extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FinancialYearStatus status = FinancialYearStatus.DRAFT;

    @Column(name = "is_current", nullable = false)
    private boolean isCurrent = false;

    @OneToMany(mappedBy = "financialYear", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("periodNumber ASC")
    private List<AccountingPeriod> periods = new ArrayList<>();

    // ===== Closing =====

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Column(name = "closing_journal_id")
    private Long closingJournalId;

    // ===== Reopening =====

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reopened_by")
    private User reopenedBy;

    @Column(name = "reopen_reason", length = 500)
    private String reopenReason;

    // ===== Closing snapshot (preserved for historical reporting even if account data changes) =====

    @Column(name = "total_revenue", precision = 19, scale = 2)
    private BigDecimal totalRevenue;

    @Column(name = "total_expense", precision = 19, scale = 2)
    private BigDecimal totalExpense;

    @Column(name = "net_profit_loss", precision = 19, scale = 2)
    private BigDecimal netProfitLoss;

    @Column(name = "retained_earnings_movement", precision = 19, scale = 2)
    private BigDecimal retainedEarningsMovement;

    @Column(name = "has_opening_balance", nullable = false)
    private boolean hasOpeningBalance = false;
}
