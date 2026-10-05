package com.unionsg.xaccounting.entity.downpayment;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Part or all of a downpayment's available balance returned in cash: paid back to the
 * customer, or received back from the supplier. Reversal flags the row and posts a reversing
 * journal rather than deleting it.
 */
@Entity
@Table(name = "downpayment_refunds", indexes = {
        @Index(name = "idx_dp_refund_downpayment", columnList = "downpayment_id")
})
@Getter
@Setter
public class DownpaymentRefund extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "downpayment_id")
    private Downpayment downpayment;

    @Column(name = "refund_number", nullable = false, length = 120)
    private String refundNumber;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "refund_date", nullable = false)
    private LocalDate refundDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id")
    private BankAccount bankAccount;

    @Column(length = 150)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @Column(nullable = false)
    private Boolean reversed = false;

    private LocalDateTime reversedAt;

    @Column(name = "reversal_reason", columnDefinition = "TEXT")
    private String reversalReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_journal_id")
    private JournalEntry reversalJournal;
}
