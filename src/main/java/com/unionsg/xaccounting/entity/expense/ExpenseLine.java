package com.unionsg.xaccounting.entity.expense;

import com.unionsg.xaccounting.entity.AccountEntity;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One line of an expense: what was spent, its category and the expense account it is booked to. */
@Entity
@Getter
@Setter
@Table(
        name = "expense_lines",
        indexes = {
                @Index(name = "idx_expense_line_expense", columnList = "expense_id"),
                @Index(name = "idx_expense_line_account", columnList = "account_id")
        },
        check = @CheckConstraint(name = "ck_expense_line_amount_positive", constraint = "amount > 0")
)
public class ExpenseLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    /** When the cost was incurred, if different from the payment date. */
    @Column(name = "expense_date")
    private LocalDate expenseDate;

    /** Item of the "expense-categories" configuration, stored by code, for reporting. */
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    /** The expense account the line is debited to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    /** In the expense's currency. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount;
}
