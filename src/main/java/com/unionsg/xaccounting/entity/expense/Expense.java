package com.unionsg.xaccounting.entity.expense;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.expense.ExpenseStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An expense paid straight from one of the organization's bank or cash accounts (no bill).
 * Posting it creates one balanced journal through the shared journal service: each line debits
 * its expense account and the payment account's GL is credited with the total. Reversing it
 * posts a reversal journal and never touches the original.
 *
 * <p>Amounts are in the payment account's currency; the {@code base*} columns hold the same
 * values in the organization's base (accounting) currency, which is what the journal is posted
 * in.</p>
 */
@Entity
@Getter
@Setter
@Table(
        name = "expenses",
        indexes = {
                @Index(name = "idx_expense_payment_date", columnList = "payment_date"),
                @Index(name = "idx_expense_status", columnList = "status"),
                @Index(name = "idx_expense_supplier", columnList = "supplier_id"),
                @Index(name = "idx_expense_payment_account", columnList = "payment_account_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_expense_number", columnNames = "expense_number"),
                @UniqueConstraint(name = "uk_expense_journal", columnNames = "journal_id"),
                @UniqueConstraint(name = "uk_expense_reversal_journal", columnNames = "reversal_journal_id")
        },
        check = {
                @CheckConstraint(name = "ck_expense_total_positive", constraint = "total_amount > 0"),
                @CheckConstraint(name = "ck_expense_rate_positive", constraint = "exchange_rate > 0"),
                @CheckConstraint(name = "ck_expense_posted_has_journal",
                        constraint = "status NOT IN ('POSTED', 'REVERSED') OR journal_id IS NOT NULL"),
                @CheckConstraint(name = "ck_expense_reversed_has_journal",
                        constraint = "status <> 'REVERSED' OR reversal_journal_id IS NOT NULL")
        }
)
public class Expense extends BaseEntity {

    @Column(name = "expense_number", nullable = false, length = 50, updatable = false)
    private String expenseNumber;

    /** External reference, e.g. the supplier's receipt or invoice number. */
    @Column(name = "reference", length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** The bank or cash account the expense was paid from. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_account_id", nullable = false)
    private BankAccount paymentAccount;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /** The payment account's currency. */
    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "base_currency", nullable = false, length = 10)
    private String baseCurrency;

    /** 1 unit of {@code currency} = {@code exchangeRate} units of base currency. */
    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 10)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "base_total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseTotalAmount;

    @Column(name = "memo", length = 1000)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.DRAFT;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber ASC")
    private List<ExpenseLine> lines = new ArrayList<>();

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
}
