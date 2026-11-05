package com.unionsg.xaccounting.entity.branch;

import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Single-row, organization-wide switches for branch accounting and journal approvals. */
@Entity
@Table(name = "branch_accounting_settings")
@Getter
@Setter
public class BranchAccountingSettings extends BaseEntity {

    /** Every journal must balance per branch; cross-branch journals get inter-branch lines. */
    @Column(name = "self_balancing_branches", nullable = false)
    private Boolean selfBalancingBranches = true;

    /** GL account the inter-branch (due to / due from) lines are posted to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inter_branch_account_id")
    private AccountEntity interBranchAccount;

    /** Manual journals must be submitted and approved before they can be posted. */
    @Column(name = "journal_approval_required", nullable = false)
    private Boolean journalApprovalRequired = true;

    /** Only manual journals totalling at least this need approval; null means all of them. */
    @Column(name = "approval_threshold", precision = 19, scale = 2)
    private BigDecimal approvalThreshold;

    /**
     * Lets manual journals post to control accounts (payables, receivables, payroll) and lets
     * their lines name an employee, vendor or customer and the bill or invoice they settle.
     * Off by default: such journals don't update the Bills, Invoices, Payments or Payroll
     * records, so those screens and their reports can disagree with the ledger.
     */
    @Column(name = "subledger_journals_enabled", nullable = false, columnDefinition = "boolean default false not null")
    private Boolean subledgerJournalsEnabled = false;

    /** Set once existing users have been given Global scope, so later users aren't. */
    @Column(name = "scope_backfill_done", nullable = false, columnDefinition = "boolean default false not null")
    private Boolean scopeBackfillDone = false;
}
