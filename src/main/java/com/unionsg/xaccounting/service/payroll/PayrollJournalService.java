package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.journal.JournalResponse;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payroll.EmployeePayrollRecord;
import com.unionsg.xaccounting.entity.payroll.PayComponent;
import com.unionsg.xaccounting.entity.payroll.PayrollRecordComponent;
import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.entity.payroll.StatutoryScheme;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.PayComponentSide;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.journal.JournalEntryRepository;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.journal.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Posts a Payroll Run's calculated result to the General Ledger (§23-§26) - the single place in
 * the module that is allowed to touch {@code JournalService}. Every earning debits the expense
 * account its {@link PayComponent} (or the run-level default, for Basic Salary) is configured
 * for; every employee deduction credits its own liability or asset-reduction account; every
 * employer contribution posts both its expense and its liability leg. What is <em>not</em> posted
 * per-component is the employee's own net pay - the whole run's net pay is credited once to the
 * single Salary Payable control account (§26), exactly the way §23's example lays it out.
 *
 * <p>Respects {@link PeriodLockGuard} exactly like every other posting path in the system (§36):
 * a payroll run dated into a locked or closed accounting period is refused before anything is
 * written.</p>
 */
@Service
@RequiredArgsConstructor
public class PayrollJournalService {

    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;
    private final PeriodLockGuard periodLockGuard;
    private final EmployeeLoanService employeeLoanService;
    private final SalaryAdvanceService salaryAdvanceService;

    @Value("${payroll.journal.salary-payable-account-id}")
    private String salaryPayableAccountId;

    @Value("${payroll.journal.default-salary-expense-account-id}")
    private String defaultSalaryExpenseAccountId;

    @Value("${payroll.journal.employee-tax-payable-account-id}")
    private String employeeTaxPayableAccountId;

    @Value("${payroll.loan.receivable-account-id}")
    private String loanReceivableAccountId;

    @Value("${payroll.advance.receivable-account-id}")
    private String advanceReceivableAccountId;

    @Transactional
    public JournalResponse postRun(PayrollRun run) {
        periodLockGuard.assertPostable(run.getPayrollCalendarPeriod().getAccountingDate());

        Map<String, BigDecimal> debits = new LinkedHashMap<>();
        Map<String, BigDecimal> credits = new LinkedHashMap<>();
        BigDecimal totalNetPay = BigDecimal.ZERO;

        for (EmployeePayrollRecord record : run.getRecords()) {
            totalNetPay = totalNetPay.add(record.getNetPay());
            for (PayrollRecordComponent comp : record.getComponents()) {
                applyComponent(comp, debits, credits);
            }
        }

        if (totalNetPay.compareTo(BigDecimal.ZERO) > 0) {
            credits.merge(salaryPayableAccountId, totalNetPay, BigDecimal::add);
        }

        java.util.List<CreateJournalLineRequest> lines = new java.util.ArrayList<>();
        debits.forEach((accountCode, amount) -> lines.add(CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountCode))
                .description("Payroll run " + run.getRunNumber())
                .debitAmount(amount.setScale(2, RoundingMode.HALF_UP))
                .creditAmount(BigDecimal.ZERO)
                .build()));
        credits.forEach((accountCode, amount) -> lines.add(CreateJournalLineRequest.builder()
                .accountId(resolveAccountId(accountCode))
                .description("Payroll run " + run.getRunNumber())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(amount.setScale(2, RoundingMode.HALF_UP))
                .build()));

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(run.getPayrollCalendarPeriod().getAccountingDate())
                .reference(run.getRunNumber())
                .description("Payroll run " + run.getRunNumber() + " for "
                        + run.getPayrollGroup().getName() + " ("
                        + run.getPayrollCalendarPeriod().getPeriodStart() + " to "
                        + run.getPayrollCalendarPeriod().getPeriodEnd() + ")")
                .journalType(JournalType.PAYROLL)
                .lines(lines)
                .build();

        JournalResponse created = journalService.create(request);
        JournalEntry entry = journalEntryRepository.findById(created.getId())
                .orElseThrow(() -> new BusinessException("Journal not found after creation"));
        entry.setSourceModule("PAYROLL_RUN");
        entry.setSourceEntityId(run.getId());
        journalEntryRepository.save(entry);
        JournalResponse posted = journalService.post(created.getId());

        // Keep the loan/advance subledger in step with the GL the moment the reducing entry
        // posts (§25) - the whole repayment is applied to principal, since the calculation
        // engine does not yet split an installment into principal/interest (see its class doc).
        for (EmployeePayrollRecord record : run.getRecords()) {
            for (PayrollRecordComponent comp : record.getComponents()) {
                String desc = comp.getDescription();
                if (desc == null) {
                    continue;
                }
                if (desc.startsWith("EMPLOYEE_LOAN:")) {
                    Long loanId = Long.valueOf(desc.substring("EMPLOYEE_LOAN:".length()));
                    employeeLoanService.applyRepayment(loanId, comp.getAmount(), BigDecimal.ZERO);
                } else if (desc.startsWith("SALARY_ADVANCE:")) {
                    Long advanceId = Long.valueOf(desc.substring("SALARY_ADVANCE:".length()));
                    salaryAdvanceService.applyRepayment(advanceId, comp.getAmount());
                }
            }
        }

        return posted;
    }

    private void applyComponent(PayrollRecordComponent comp, Map<String, BigDecimal> debits, Map<String, BigDecimal> credits) {
        PayComponent payComponent = comp.getPayComponent();
        StatutoryScheme scheme = comp.getStatutoryScheme();
        BigDecimal amount = comp.getAmount();

        if (scheme != null) {
            if (comp.getSide() == PayComponentSide.EMPLOYEE_DEDUCTION) {
                credits.merge(requireAccount(scheme.getEmployeeLiabilityAccountCode(), "statutory scheme " + scheme.getCode()), amount, BigDecimal::add);
            } else if (comp.getSide() == PayComponentSide.EMPLOYER_CONTRIBUTION) {
                debits.merge(requireAccount(scheme.getEmployerExpenseAccountCode(), "statutory scheme " + scheme.getCode()), amount, BigDecimal::add);
                credits.merge(requireAccount(scheme.getEmployerLiabilityAccountCode(), "statutory scheme " + scheme.getCode()), amount, BigDecimal::add);
            }
            return;
        }

        if (payComponent != null) {
            if (comp.getSide() == PayComponentSide.EARNING) {
                debits.merge(requireAccount(payComponent.getGlDebitAccountCode(), payComponent.getName()), amount, BigDecimal::add);
            } else if (comp.getSide() == PayComponentSide.EMPLOYEE_DEDUCTION) {
                credits.merge(requireAccount(payComponent.getGlCreditAccountCode(), payComponent.getName()), amount, BigDecimal::add);
            } else if (comp.getSide() == PayComponentSide.EMPLOYER_CONTRIBUTION) {
                debits.merge(requireAccount(payComponent.getGlDebitAccountCode(), payComponent.getName()), amount, BigDecimal::add);
                credits.merge(requireAccount(payComponent.getGlCreditAccountCode(), payComponent.getName()), amount, BigDecimal::add);
            }
            return;
        }

        // Components with neither a PayComponent nor a StatutoryScheme are the run-level
        // built-ins: Basic Salary, Income Tax, and loan/advance repayments (see class doc and
        // PayrollCalculationService).
        String desc = comp.getDescription();
        if ("Basic Salary".equals(comp.getComponentName())) {
            debits.merge(defaultSalaryExpenseAccountId, amount, BigDecimal::add);
        } else if ("Income Tax".equals(comp.getComponentName())) {
            credits.merge(employeeTaxPayableAccountId, amount, BigDecimal::add);
        } else if (desc != null && desc.startsWith("EMPLOYEE_LOAN:")) {
            credits.merge(loanReceivableAccountId, amount, BigDecimal::add);
        } else if (desc != null && desc.startsWith("SALARY_ADVANCE:")) {
            credits.merge(advanceReceivableAccountId, amount, BigDecimal::add);
        } else {
            throw new BusinessException("Payroll component \"" + comp.getComponentName()
                    + "\" has no Chart-of-Accounts mapping configured - cannot post payroll (§56)");
        }
    }

    private String requireAccount(String accountCode, String context) {
        if (accountCode == null || accountCode.isBlank()) {
            throw new BusinessException("No GL account configured for " + context + " - cannot post payroll (§53, §56)");
        }
        return accountCode;
    }

    private Long resolveAccountId(String accountCode) {
        return accountRepository.findByAccountId(accountCode)
                .map(a -> Long.valueOf(a.getAccountId()))
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountCode));
    }
}
