package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.bankrec.MatchStatus;
import com.unionsg.xaccounting.enums.bankrec.MatchType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Links one or more statement lines to one or more posted bank GL lines. The amounts on each
 * side always balance; a partial match simply allocates less than an item's full amount.
 * Unmatching keeps the row (status UNMATCHED) so the audit trail shows what was undone.
 */
@Entity
@Table(name = "bank_reconciliation_matches", indexes = {
        @Index(name = "idx_bank_rec_match_rec", columnList = "reconciliation_id"),
        @Index(name = "idx_bank_rec_match_status", columnList = "status")
})
@Getter
@Setter
public class BankReconciliationMatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reconciliation_id", nullable = false)
    private BankReconciliation reconciliation;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 20)
    private MatchType matchType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    /** Engine score from 0 to 100; null for manual matches. */
    private Integer confidence;

    @Column(name = "rule_name", length = 150)
    private String ruleName;

    @Column(name = "match_reasons", length = 500)
    private String matchReasons;

    @Column(name = "matched_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "matched_by_name", length = 150)
    private String matchedByName;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "confirmed_by_name", length = 150)
    private String confirmedByName;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "unmatched_by_name", length = 150)
    private String unmatchedByName;

    @Column(name = "unmatched_at")
    private LocalDateTime unmatchedAt;

    @Column(name = "unmatch_reason", length = 500)
    private String unmatchReason;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BankReconciliationMatchItem> items = new ArrayList<>();

    public void addItem(BankReconciliationMatchItem item) {
        item.setMatch(this);
        items.add(item);
    }
}
