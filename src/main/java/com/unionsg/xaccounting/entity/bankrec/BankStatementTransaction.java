package com.unionsg.xaccounting.entity.bankrec;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.enums.bankrec.StatementTransactionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A line from an imported bank statement. Kept apart from the ledger: it never posts anything
 * and only becomes part of the books through a match or a reconciliation adjustment.
 * {@code amount} is signed from the bank account's point of view (deposits positive).
 */
@Entity
@Table(name = "bank_statement_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_bank_statement_txn_dedupe",
                columnNames = {"bank_account_id", "dedupe_key"}),
        indexes = {
                @Index(name = "idx_bank_statement_txn_account_date", columnList = "bank_account_id, transaction_date"),
                @Index(name = "idx_bank_statement_txn_status", columnList = "status")
        })
@Getter
@Setter
public class BankStatementTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_import_id", nullable = false)
    private BankStatementImport statementImport;

    @Column(name = "source_row_number")
    private Integer sourceRowNumber;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(length = 500)
    private String description;

    @Column(length = 150)
    private String reference;

    @Column(name = "debit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "running_balance", precision = 19, scale = 2)
    private BigDecimal runningBalance;

    @Column(name = "external_transaction_id", length = 150)
    private String externalTransactionId;

    @Column(name = "dedupe_key", nullable = false, length = 64)
    private String dedupeKey;

    /** Absolute amount currently held by active (proposed or confirmed) matches. */
    @Column(name = "matched_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatementTransactionStatus status = StatementTransactionStatus.UNMATCHED;

    /** Set when the reconciliation that cleared this line is completed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleared_in_reconciliation_id")
    private BankReconciliation clearedInReconciliation;

    public BigDecimal getAbsoluteAmount() {
        return amount == null ? BigDecimal.ZERO : amount.abs();
    }

    public BigDecimal getRemainingAmount() {
        return getAbsoluteAmount().subtract(matchedAmount == null ? BigDecimal.ZERO : matchedAmount);
    }
}
