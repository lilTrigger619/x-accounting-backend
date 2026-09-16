package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.entity.User.User;
import com.unionsg.xaccounting.enums.PayrollInputSourceType;
import com.unionsg.xaccounting.enums.PayrollInputStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A variable, one-off payroll input entered or imported for a specific employee and period
 * (§12, §13, §18) - overtime, a bonus, a commission, a one-off allowance, a manual deduction, or
 * an ad-hoc adjustment/arrears amount. Requires approval before it is picked up by payroll
 * calculation (§18), and is applied to at most one Payroll Run - {@code appliedToRunId} records
 * which, guarding against the same overtime or bonus being paid twice.
 */
@Entity
@Table(name = "payroll_inputs")
@Getter
@Setter
public class PayrollInput extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_calendar_period_id", nullable = false)
    private PayrollCalendarPeriod payrollCalendarPeriod;

    @Enumerated(EnumType.STRING)
    private PayrollInputSourceType sourceType;

    /** Which pay component this input posts as (e.g. the "Overtime" or "Sales Commission" component). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pay_component_id", nullable = false)
    private PayComponent payComponent;

    private String description;

    /** Direct amount. Null when this is an OVERTIME input computed from hours/rate/multiplier. */
    private BigDecimal amount;

    private BigDecimal hours;

    private BigDecimal rate;

    private BigDecimal multiplier;

    @Enumerated(EnumType.STRING)
    private PayrollInputStatus status = PayrollInputStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private LocalDateTime approvedAt;

    /** Set once this input has actually been folded into a calculated Payroll Run. */
    private Long appliedToRunId;

    public BigDecimal resolveAmount() {
        if (amount != null) {
            return amount;
        }
        if (hours != null && rate != null) {
            BigDecimal m = multiplier != null ? multiplier : BigDecimal.ONE;
            return hours.multiply(rate).multiply(m);
        }
        return BigDecimal.ZERO;
    }
}
