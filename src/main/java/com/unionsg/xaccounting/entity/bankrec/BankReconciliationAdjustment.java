package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.enums.bankrec.AdjustmentStatus;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAdjustmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A bank-only item (charges, interest, direct debits/credits, unknown items) brought into the
 * ledger from a reconciliation. This is the only part of reconciliation that posts a journal.
 */
@Entity
@Table(name = "bank_reconciliation_adjustments")
@Getter
@Setter
public class BankReconciliationAdjustment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private BankReconciliation reconciliation;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 30)
    private ReconciliationAdjustmentType adjustmentType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /** Always positive; the type decides whether it is money into or out of the bank. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String description;

    @Column(length = 150)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offset_account_id", nullable = false)
    private AccountEntity offsetAccount;

    /** The statement line this adjustment brings into the books, when there is one. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_transaction_id")
    private BankStatementTransaction statementTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id")
    private BankReconciliationMatch match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdjustmentStatus status = AdjustmentStatus.POSTED;

    @Column(name = "posted_by_name", length = 150)
    private String postedByName;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "reversal_journal_id")
    private Long reversalJournalId;

    @Column(name = "reversed_by_name", length = 150)
    private String reversedByName;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    /** Signed effect on the bank balance: positive for money in, negative for money out. */
    public BigDecimal getSignedAmount() {
        return adjustmentType.isMoneyIn() ? amount : amount.negate();
    }
}
