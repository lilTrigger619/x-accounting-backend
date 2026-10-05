package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One reconciliation of a {@link BankAccount}'s ledger balance against an external bank
 * statement for a period. The balance fields are recalculated on every change while the
 * reconciliation is open and frozen when it is completed, so a RECONCILED row is the record of
 * what was signed off.
 */
@Entity
@Table(name = "bank_reconciliations", indexes = {
        @Index(name = "idx_bank_rec_bank_account", columnList = "bank_account_id"),
        @Index(name = "idx_bank_rec_status", columnList = "status")
})
@Getter
@Setter
public class BankReconciliation extends BaseEntity {

    @Column(name = "reconciliation_number", nullable = false, unique = true, length = 60)
    private String reconciliationNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @Column(name = "statement_date", nullable = false)
    private LocalDate statementDate;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(length = 10)
    private String currency;

    @Column(name = "opening_bank_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBankBalance = BigDecimal.ZERO;

    @Column(name = "closing_bank_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal closingBankBalance = BigDecimal.ZERO;

    /** Ledger balance of the bank's GL account at period end, including posted adjustments. */
    @Column(name = "book_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal bookBalance = BigDecimal.ZERO;

    @Column(name = "outstanding_deposits", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingDeposits = BigDecimal.ZERO;

    @Column(name = "outstanding_withdrawals", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingWithdrawals = BigDecimal.ZERO;

    @Column(name = "bank_charges", nullable = false, precision = 19, scale = 2)
    private BigDecimal bankCharges = BigDecimal.ZERO;

    @Column(name = "bank_interest", nullable = false, precision = 19, scale = 2)
    private BigDecimal bankInterest = BigDecimal.ZERO;

    /** Net effect of every posted adjustment on the bank balance (money in positive). */
    @Column(name = "adjustments_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal adjustmentsTotal = BigDecimal.ZERO;

    @Column(name = "difference", nullable = false, precision = 19, scale = 2)
    private BigDecimal difference = BigDecimal.ZERO;

    /** Largest absolute difference that still counts as reconciled. */
    @Column(name = "tolerance", nullable = false, precision = 19, scale = 2)
    private BigDecimal tolerance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationStatus status = ReconciliationStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "prepared_by_id", length = 40)
    private String preparedById;

    @Column(name = "prepared_by_name", length = 150)
    private String preparedByName;

    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "reviewed_by_id", length = 40)
    private String reviewedById;

    @Column(name = "reviewed_by_name", length = 150)
    private String reviewedByName;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "completed_by_id", length = 40)
    private String completedById;

    @Column(name = "completed_by_name", length = 150)
    private String completedByName;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "difference_overridden", nullable = false)
    private Boolean differenceOverridden = false;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @Column(name = "reopened_by_id", length = 40)
    private String reopenedById;

    @Column(name = "reopened_by_name", length = 150)
    private String reopenedByName;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "reopen_reason", columnDefinition = "TEXT")
    private String reopenReason;

    @Column(name = "reopen_count", nullable = false)
    private Integer reopenCount = 0;

    @Column(name = "cancelled_by_name", length = 150)
    private String cancelledByName;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "last_auto_match_at")
    private LocalDateTime lastAutoMatchAt;
}
