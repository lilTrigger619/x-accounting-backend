package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * A configurable automatic-matching rule. Every rule requires the amounts to be equal; the
 * other criteria raise the confidence score. Pairs scoring at or above
 * {@code autoConfirmThreshold} are matched straight away, pairs between
 * {@code suggestThreshold} and that are only proposed for a user to confirm.
 */
@Entity
@Table(name = "bank_matching_rules")
@Getter
@Setter
public class BankMatchingRule extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    /** Lower numbers run first. */
    @Column(nullable = false)
    private Integer priority = 100;

    @Column(nullable = false)
    private Boolean active = true;

    /** Limits the rule to one bank account; null applies it to every account. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(name = "date_tolerance_days", nullable = false)
    private Integer dateToleranceDays = 3;

    @Column(name = "match_reference", nullable = false)
    private Boolean matchReference = true;

    @Column(name = "match_transaction_number", nullable = false)
    private Boolean matchTransactionNumber = true;

    @Column(name = "match_description", nullable = false)
    private Boolean matchDescription = true;

    @Column(name = "match_cheque_number", nullable = false)
    private Boolean matchChequeNumber = true;

    @Column(name = "match_counterparty", nullable = false)
    private Boolean matchCounterparty = true;

    @Column(name = "auto_confirm_threshold", nullable = false)
    private Integer autoConfirmThreshold = 85;

    @Column(name = "suggest_threshold", nullable = false)
    private Integer suggestThreshold = 60;
}
