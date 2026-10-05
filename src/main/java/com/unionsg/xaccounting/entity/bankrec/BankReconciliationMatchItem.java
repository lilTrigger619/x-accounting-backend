package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalLine;
import com.unionsg.xaccounting.enums.bankrec.MatchItemSide;
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

/** One side of a match: a statement line or a posted bank GL line, and how much of it is used. */
@Entity
@Table(name = "bank_reconciliation_match_items", indexes = {
        @Index(name = "idx_bank_rec_item_match", columnList = "match_id"),
        @Index(name = "idx_bank_rec_item_statement", columnList = "statement_transaction_id"),
        @Index(name = "idx_bank_rec_item_journal_line", columnList = "journal_line_id")
})
@Getter
@Setter
public class BankReconciliationMatchItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private BankReconciliationMatch match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchItemSide side;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_transaction_id")
    private BankStatementTransaction statementTransaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_line_id")
    private JournalLine journalLine;

    /** Absolute amount allocated from the item. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
}
