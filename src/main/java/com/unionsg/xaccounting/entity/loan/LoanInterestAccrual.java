package com.unionsg.xaccounting.entity.loan;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Interest recognised in the GL before it is paid: Dr Interest Expense / Cr Interest Payable
 * for a borrowed loan, Dr Interest Receivable / Cr Interest Income for a lent loan.
 */
@Entity
@Table(name = "loan_interest_accruals")
@Getter
@Setter
public class LoanInterestAccrual extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id")
    private JournalEntry journal;

    @Column(nullable = false)
    private Boolean reversed = false;
}
