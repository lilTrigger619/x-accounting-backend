package com.unionsg.xaccounting.entity.fixedasset;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.enums.fixedasset.AssetTransactionType;
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
 * One posted movement in an asset's subledger, written in the same transaction as its journal.
 * Amounts are signed effects on the asset's balances: an acquisition adds cost, depreciation adds
 * accumulated depreciation, a disposal takes both away. A reversal is a new row with the opposite
 * signs (type REVERSAL, pointing at the row it reverses), so the history is never edited and a
 * sum of rows up to any date gives the asset's balances on that date.
 */
@Entity
@Table(name = "asset_transactions", indexes = {
        @Index(name = "idx_asset_tx_asset", columnList = "asset_id"),
        @Index(name = "idx_asset_tx_date", columnList = "transaction_date")
})
@Getter
@Setter
public class AssetTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private FixedAsset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private AssetTransactionType type;

    /** For a REVERSAL: the type of the row it reverses. Otherwise the same as {@code type}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private AssetTransactionType movementType;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "cost_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal costAmount = BigDecimal.ZERO;

    @Column(name = "depreciation_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal depreciationAmount = BigDecimal.ZERO;

    @Column(name = "impairment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal impairmentAmount = BigDecimal.ZERO;

    /** Disposal proceeds (signed like the row). */
    @Column(name = "proceeds_amount", precision = 19, scale = 2)
    private BigDecimal proceedsAmount;

    /** Disposal gain (positive) or loss (negative), signed like the row. */
    @Column(name = "gain_loss_amount", precision = 19, scale = 2)
    private BigDecimal gainLossAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    /** For depreciation: the run line it came from. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_line_id")
    private DepreciationRunLine runLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reverses_id")
    private AssetTransaction reverses;

    /** Set on the original once a REVERSAL row exists for it. */
    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    /** The depreciated-through date before this row was posted, restored if it is reversed. */
    @Column(name = "previous_depreciated_through")
    private LocalDate previousDepreciatedThrough;

    @Column(length = 500)
    private String description;

    public boolean isReversed() {
        return reversedAt != null;
    }
}
