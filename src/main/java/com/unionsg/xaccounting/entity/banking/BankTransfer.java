package com.unionsg.xaccounting.entity.banking;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.banking.BankTransferStatus;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Movement of funds between two of the organization's own bank/cash accounts. Posting it
 * creates one balanced journal (Dr destination, Cr source, plus fee and exchange-difference
 * lines) through the shared journal service; reversing it posts a reversal journal and never
 * touches the original.
 *
 * <p>Amounts: {@code amount} and {@code feeAmount} are in the source account's currency,
 * {@code convertedAmount} is in the destination account's currency. The {@code base*} columns
 * hold the same values in the organization's base (accounting) currency, which is what the
 * journal is posted in.</p>
 */
@Entity
@Getter
@Setter
@Table(
        name = "bank_transfers",
        indexes = {
                @Index(name = "idx_bank_transfer_date", columnList = "transfer_date"),
                @Index(name = "idx_bank_transfer_status", columnList = "status"),
                @Index(name = "idx_bank_transfer_source", columnList = "source_bank_account_id"),
                @Index(name = "idx_bank_transfer_destination", columnList = "destination_bank_account_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bank_transfer_number", columnNames = "transfer_number"),
                @UniqueConstraint(name = "uk_bank_transfer_journal", columnNames = "journal_id"),
                @UniqueConstraint(name = "uk_bank_transfer_reversal_journal", columnNames = "reversal_journal_id")
        },
        check = {
                @CheckConstraint(name = "ck_bank_transfer_distinct_accounts",
                        constraint = "source_bank_account_id <> destination_bank_account_id"),
                @CheckConstraint(name = "ck_bank_transfer_amount_positive", constraint = "amount > 0"),
                @CheckConstraint(name = "ck_bank_transfer_fee_non_negative", constraint = "fee_amount >= 0"),
                @CheckConstraint(name = "ck_bank_transfer_rate_positive", constraint = "exchange_rate > 0"),
                @CheckConstraint(name = "ck_bank_transfer_posted_has_journal",
                        constraint = "status NOT IN ('POSTED', 'REVERSED') OR journal_id IS NOT NULL"),
                @CheckConstraint(name = "ck_bank_transfer_reversed_has_journal",
                        constraint = "status <> 'REVERSED' OR reversal_journal_id IS NOT NULL")
        }
)
public class BankTransfer extends BaseEntity {

    @Column(name = "transfer_number", nullable = false, length = 50, updatable = false)
    private String transferNumber;

    /** External reference, e.g. the bank's transaction or cheque number. */
    @Column(name = "reference", length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_bank_account_id", nullable = false)
    private BankAccount sourceBankAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_bank_account_id", nullable = false)
    private BankAccount destinationBankAccount;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "source_currency", nullable = false, length = 10)
    private String sourceCurrency;

    @Column(name = "destination_currency", nullable = false, length = 10)
    private String destinationCurrency;

    /** 1 unit of source currency = {@code exchangeRate} units of destination currency. */
    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /** Amount received by the destination account, in its currency. */
    @Column(name = "converted_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal convertedAmount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    /** Overrides {@code MappingKey.BANK_TRANSFER_CHARGES} when set. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_account_id")
    private AccountEntity feeAccount;

    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency;

    /** 1 unit of source currency = {@code sourceBaseRate} units of base currency. */
    @Column(name = "source_base_rate", nullable = false, precision = 19, scale = 10)
    private BigDecimal sourceBaseRate = BigDecimal.ONE;

    /** 1 unit of destination currency = {@code destinationBaseRate} units of base currency. */
    @Column(name = "destination_base_rate", nullable = false, precision = 19, scale = 10)
    private BigDecimal destinationBaseRate = BigDecimal.ONE;

    @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Column(name = "base_converted_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseConvertedAmount = BigDecimal.ZERO;

    @Column(name = "base_fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseFeeAmount = BigDecimal.ZERO;

    /** Positive for a gain, negative for a loss, in base currency. */
    @Column(name = "exchange_gain_loss", nullable = false, precision = 19, scale = 2)
    private BigDecimal exchangeGainLoss = BigDecimal.ZERO;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BankTransferStatus status = BankTransferStatus.DRAFT;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reversal_journal_id")
    private JournalEntry reversalJournal;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(name = "posted_by", length = 100)
    private String postedBy;

    @Column(name = "reversed_at")
    private LocalDateTime reversedAt;

    @Column(name = "reversed_by", length = 100)
    private String reversedBy;

    @Column(name = "reversal_reason", length = 500)
    private String reversalReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by", length = 100)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
}
