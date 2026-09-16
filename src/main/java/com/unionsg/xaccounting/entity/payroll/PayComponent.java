package com.unionsg.xaccounting.entity.payroll;

import com.unionsg.xaccounting.entity.BaseEntity;
import com.unionsg.xaccounting.enums.PayComponentCalculationMethod;
import com.unionsg.xaccounting.enums.PayComponentCategory;
import com.unionsg.xaccounting.enums.PayComponentSide;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single configurable payroll line item - an earning, an employee deduction, an employer
 * contribution, or a benefit (§7, §8, §9). This is the mechanism that satisfies §33/§53: every
 * component carries its own Chart-of-Accounts mapping, so posting a payroll run never requires a
 * developer to hard-code an account number for a new allowance or deduction type.
 *
 * <p>{@code glDebitAccountCode} is used when this component is debited as an expense (every
 * EARNING, and the employer's own cost for an EMPLOYER_CONTRIBUTION). {@code glCreditAccountCode}
 * is used when this component is credited to a liability, or to an asset-reduction account, such
 * as Employee Tax Payable, a statutory scheme's payable account, or the Employee Loan Receivable
 * account being paid down by a repayment deduction. A plain EARNING's net effect on what is owed
 * to the employee is not posted per-component - it rolls into the single Salary Payable control
 * account at the Payroll Run level (§26), matching how a Sales invoice's net effect rolls into a
 * single Accounts Receivable control account rather than one receivable account per line item.</p>
 */
@Entity
@Table(name = "pay_components")
@Getter
@Setter
public class PayComponent extends BaseEntity {

    private String code;

    private String name;

    @Enumerated(EnumType.STRING)
    private PayComponentCategory category;

    @Enumerated(EnumType.STRING)
    private PayComponentSide side;

    @Enumerated(EnumType.STRING)
    private PayComponentCalculationMethod calculationMethod;

    /** Default fixed amount, or default percentage (as a whole number, e.g. 5 = 5%). */
    private BigDecimal defaultValue;

    /** Whether this earning counts toward taxable income, or this deduction is taken pre-tax. */
    private boolean taxable = true;

    /** Whether this earning counts toward the pension/social-security contribution base. */
    private boolean pensionable = true;

    /** A mandatory statutory withholding rather than a discretionary company policy. */
    private boolean statutory = false;

    /** Expected on every payroll run (e.g. Basic Salary) vs. a one-off/variable input. */
    private boolean recurring = true;

    private boolean active = true;

    /** Chart-of-Accounts code debited when this component is applied (expense side). */
    private String glDebitAccountCode;

    /** Chart-of-Accounts code credited when this component is applied (liability/asset side). */
    private String glCreditAccountCode;

    private String description;
}
