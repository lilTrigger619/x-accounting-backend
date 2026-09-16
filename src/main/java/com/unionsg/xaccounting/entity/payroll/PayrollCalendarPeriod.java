package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.accounting.AccountingPeriod;
import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import com.unionsg.xaccounting.enums.PayFrequency;
import com.unionsg.xaccounting.enums.PayrollPeriodStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * One payroll period on the Payroll Calendar for a given Payroll Group (§5) - the period the
 * employees actually worked, the date they're scheduled to be paid, and the accounting date the
 * resulting journal is posted on. That accounting date is what {@code PayrollRunService} checks
 * against {@code PeriodLockGuard} and the linked {@link AccountingPeriod}/{@link FinancialYear}
 * (§36): a payroll period is never allowed to post into a locked or closed accounting period, or
 * a Financial Year that isn't open for posting.
 */
@Entity
@Table(name = "payroll_calendar_periods")
@Getter
@Setter
public class PayrollCalendarPeriod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_group_id", nullable = false)
    private PayrollGroup payrollGroup;

    @Enumerated(EnumType.STRING)
    private PayFrequency payFrequency;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private LocalDate payDate;

    /** The date the payroll journal is dated - resolved against AccountingPeriod/FinancialYear. */
    private LocalDate accountingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriod accountingPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_year_id")
    private FinancialYear financialYear;

    @Enumerated(EnumType.STRING)
    private PayrollPeriodStatus status = PayrollPeriodStatus.OPEN;
}
