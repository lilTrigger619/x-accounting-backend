package com.unionsg.xaccounting.dto.payroll;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Reconciles the payroll subledger (unpaid Salary Payable across POSTED-but-not-yet-PAID runs)
 * against the General Ledger's own Salary Payable control-account balance (§25, §43). The two
 * are expected to agree exactly, by construction, since the payroll module posts through the same
 * journal engine as everything else - a nonzero {@code difference} means something posted to the
 * Salary Payable account outside the payroll module's own flows.
 */
@Getter
@Builder
public class PayrollGlReconciliationResponse {
    private LocalDate asOfDate;
    private BigDecimal subledgerOutstandingSalaryPayable;
    private BigDecimal glSalaryPayableBalance;
    private BigDecimal difference;
}
