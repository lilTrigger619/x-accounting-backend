package com.unionsg.xaccounting.entity.expense;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.expense.ExpenseAction;
import com.unionsg.xaccounting.enums.expense.ExpenseStatus;
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

/** Append-only history of an expense. Who and when come from {@link BaseEntity}'s createdBy/createdAt. */
@Entity
@Getter
@Setter
@Table(
        name = "expense_activities",
        indexes = @Index(name = "idx_expense_activity_expense", columnList = "expense_id")
)
public class ExpenseActivity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false, updatable = false)
    private Expense expense;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30, updatable = false)
    private ExpenseAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20, updatable = false)
    private ExpenseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20, updatable = false)
    private ExpenseStatus toStatus;

    @Column(name = "details", length = 1000, updatable = false)
    private String details;
}
