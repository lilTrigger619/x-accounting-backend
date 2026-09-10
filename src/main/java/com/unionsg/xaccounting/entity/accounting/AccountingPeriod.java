package com.unionsg.xaccounting.entity.accounting;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A single accounting period (typically a calendar month) within a FinancialYear.
 * Every date within an active FinancialYear should map to exactly one AccountingPeriod.
 */
@Entity
@Table(name = "accounting_periods")
@Getter
@Setter
public class AccountingPeriod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financial_year_id", nullable = false)
    private FinancialYear financialYear;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountingPeriodStatus status = AccountingPeriodStatus.OPEN;

    /** The single period considered "current" for day-to-day data entry defaults. */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    // ===== Locking =====

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    private User lockedBy;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unlocked_by")
    private User unlockedBy;

    @Column(name = "reopen_reason", length = 500)
    private String reopenReason;

    // ===== Permanent closing =====

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    public boolean isPostable() {
        return status == AccountingPeriodStatus.OPEN;
    }
}
