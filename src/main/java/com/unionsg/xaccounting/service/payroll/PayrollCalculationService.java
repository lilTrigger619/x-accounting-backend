package com.unionsg.xaccounting.service.payroll;

import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.payroll.EmployeeLoan;
import com.unionsg.xaccounting.entity.payroll.EmployeePayrollRecord;
import com.unionsg.xaccounting.entity.payroll.EmployeeSalaryStructure;
import com.unionsg.xaccounting.entity.payroll.PayComponent;
import com.unionsg.xaccounting.entity.payroll.PayrollInput;
import com.unionsg.xaccounting.entity.payroll.PayrollRecordComponent;
import com.unionsg.xaccounting.entity.payroll.PayrollRun;
import com.unionsg.xaccounting.entity.payroll.SalaryAdvance;
import com.unionsg.xaccounting.entity.payroll.SalaryStructureLine;
import com.unionsg.xaccounting.entity.payroll.StatutoryScheme;
import com.unionsg.xaccounting.entity.payroll.TaxConfiguration;
import com.unionsg.xaccounting.enums.AdvanceStatus;
import com.unionsg.xaccounting.enums.LoanStatus;
import com.unionsg.xaccounting.enums.PayComponentCalculationMethod;
import com.unionsg.xaccounting.enums.PayComponentSide;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.payroll.EmployeeLoanRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeePayrollRecordRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeSalaryStructureRepository;
import com.unionsg.xaccounting.repository.payroll.SalaryAdvanceRepository;
import com.unionsg.xaccounting.repository.payroll.SalaryStructureLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The payroll calculation engine (§19): for every eligible employee, Gross Pay minus Employee
 * Deductions equals Net Pay, computed separately from Employer Payroll Cost. This service only
 * ever produces {@link EmployeePayrollRecord}/{@link PayrollRecordComponent} rows attached to a
 * {@code PayrollRun} still in DRAFT/CALCULATING - it never touches the General Ledger. That
 * separation (calculation vs. accounting) is enforced structurally: this class has no dependency
 * on JournalService at all.
 *
 * <p>Known simplifications, stated plainly rather than silently assumed: (1) a
 * PERCENTAGE_OF_GROSS component is computed against the sum of non-percentage-of-gross earnings,
 * not a true circular gross-up; (2) employee statutory contributions are treated as pre-tax when
 * computing taxable income for §10's bracket calculation, which is common but not universal
 * practice and should be revisited per jurisdiction; (3) a negative net pay is clamped to zero and
 * flagged rather than driving a configurable per-organization policy (§57).</p>
 */
@Service
@RequiredArgsConstructor
public class PayrollCalculationService {

    private final EmployeeSalaryStructureRepository employeeSalaryStructureRepository;
    private final SalaryStructureLineRepository salaryStructureLineRepository;
    private final StatutorySchemeService statutorySchemeService;
    private final TaxConfigurationService taxConfigurationService;
    private final PayrollInputService payrollInputService;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final SalaryAdvanceRepository salaryAdvanceRepository;
    private final EmployeePayrollRecordRepository employeePayrollRecordRepository;

    @Transactional
    public EmployeePayrollRecord calculateForEmployee(PayrollRun run, Employee employee) {
        LocalDate asOf = run.getPayrollCalendarPeriod().getPeriodEnd();

        EmployeeSalaryStructure assignment = employeeSalaryStructureRepository
                .findEffectiveAsOf(employee.getId(), asOf)
                .orElseThrow(() -> new BusinessException(
                        "Employee " + employee.getFullName() + " has no compensation assignment effective " + asOf));

        List<SalaryStructureLine> lines = salaryStructureLineRepository
                .findBySalaryStructureId(assignment.getSalaryStructure().getId());

        List<PayrollRecordComponent> components = new ArrayList<>();

        // ---- 1. Basic salary ----
        BigDecimal basicSalary = assignment.getBasicSalary();
        BigDecimal grossBeforeGrossPct = basicSalary;
        components.add(component(null, "Basic Salary", PayComponentSide.EARNING, basicSalary));

        // ---- 2. Structure earnings (fixed / % of basic), then % of gross ----
        List<SalaryStructureLine> pctOfGrossLines = new ArrayList<>();
        for (SalaryStructureLine line : lines) {
            PayComponent c = line.getPayComponent();
            if (c.getSide() != PayComponentSide.EARNING) {
                continue;
            }
            if (c.getCalculationMethod() == PayComponentCalculationMethod.PERCENTAGE_OF_GROSS) {
                pctOfGrossLines.add(line);
                continue;
            }
            BigDecimal amount = resolveLineAmount(line, basicSalary);
            grossBeforeGrossPct = grossBeforeGrossPct.add(amount);
            components.add(component(c, c.getName(), PayComponentSide.EARNING, amount));
        }
        BigDecimal grossPay = grossBeforeGrossPct;
        for (SalaryStructureLine line : pctOfGrossLines) {
            PayComponent c = line.getPayComponent();
            BigDecimal amount = grossBeforeGrossPct.multiply(line.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            grossPay = grossPay.add(amount);
            components.add(component(c, c.getName(), PayComponentSide.EARNING, amount));
        }

        // ---- 3. Approved variable inputs for this employee/period (§12, §13, §18) ----
        BigDecimal variableDeductions = BigDecimal.ZERO;
        List<PayrollInput> inputs = payrollInputService.getApprovedForPeriod(run.getPayrollCalendarPeriod().getId())
                .stream().filter(i -> i.getEmployee().getId().equals(employee.getId())).toList();
        for (PayrollInput input : inputs) {
            BigDecimal amount = input.resolveAmount();
            PayComponent c = input.getPayComponent();
            if (c.getSide() == PayComponentSide.EARNING) {
                grossPay = grossPay.add(amount);
                components.add(component(c, c.getName(), PayComponentSide.EARNING, amount));
            } else {
                variableDeductions = variableDeductions.add(amount);
                components.add(component(c, c.getName(), PayComponentSide.EMPLOYEE_DEDUCTION, amount));
            }
            payrollInputService.markApplied(input, run.getId());
        }

        // ---- 4. Taxable / pensionable bases ----
        BigDecimal taxableEarnings = basicSalary;
        BigDecimal pensionableEarnings = BigDecimal.ZERO;
        if (isTaxablePensionable(null, true)) {
            pensionableEarnings = basicSalary; // Basic Salary is always pensionable by convention.
        }
        for (SalaryStructureLine line : lines) {
            PayComponent c = line.getPayComponent();
            if (c.getSide() != PayComponentSide.EARNING) {
                continue;
            }
            BigDecimal amount = line.getPayComponent().getCalculationMethod() == PayComponentCalculationMethod.PERCENTAGE_OF_GROSS
                    ? grossBeforeGrossPct.multiply(line.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : resolveLineAmount(line, basicSalary);
            if (c.isTaxable()) {
                taxableEarnings = taxableEarnings.add(amount);
            }
            if (c.isPensionable()) {
                pensionableEarnings = pensionableEarnings.add(amount);
            }
        }

        // ---- 5. Structural employee deductions (e.g. union dues) ----
        BigDecimal structuralDeductions = BigDecimal.ZERO;
        for (SalaryStructureLine line : lines) {
            PayComponent c = line.getPayComponent();
            if (c.getSide() != PayComponentSide.EMPLOYEE_DEDUCTION) {
                continue;
            }
            BigDecimal amount = resolveLineAmount(line, basicSalary);
            structuralDeductions = structuralDeductions.add(amount);
            components.add(component(c, c.getName(), PayComponentSide.EMPLOYEE_DEDUCTION, amount));
        }

        // ---- 6. Statutory contributions, employee + employer (§11) ----
        BigDecimal employeeStatutory = BigDecimal.ZERO;
        BigDecimal employerStatutoryExpense = BigDecimal.ZERO;
        for (StatutoryScheme scheme : statutorySchemeService.getEffectiveSchemes(asOf)) {
            BigDecimal basis = switch (scheme.getCalculationBasis()) {
                case BASIC_SALARY -> basicSalary;
                case GROSS_PAY -> grossPay;
                case PENSIONABLE_EARNINGS -> pensionableEarnings;
            };
            if (scheme.getEmployeeRatePercent() != null) {
                BigDecimal amount = basis.multiply(scheme.getEmployeeRatePercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                employeeStatutory = employeeStatutory.add(amount);
                components.add(statutoryComponent(scheme, scheme.getName() + " (Employee)", PayComponentSide.EMPLOYEE_DEDUCTION, amount));
            }
            if (scheme.getEmployerRatePercent() != null) {
                BigDecimal amount = basis.multiply(scheme.getEmployerRatePercent())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                employerStatutoryExpense = employerStatutoryExpense.add(amount);
                components.add(statutoryComponent(scheme, scheme.getName() + " (Employer)", PayComponentSide.EMPLOYER_CONTRIBUTION, amount));
            }
        }

        // ---- 7. Income tax (§10) - employee statutory contributions treated as pre-tax ----
        TaxConfiguration taxConfig = taxConfigurationService.getEffectiveAsOf(asOf);
        BigDecimal taxableIncome = taxableEarnings.subtract(employeeStatutory).max(BigDecimal.ZERO);
        BigDecimal incomeTax = taxConfigurationService.computeTax(taxConfig, taxableIncome);
        if (incomeTax.compareTo(BigDecimal.ZERO) > 0) {
            components.add(component(null, "Income Tax", PayComponentSide.EMPLOYEE_DEDUCTION, incomeTax));
        }

        // ---- 8. Loan and advance repayments (§14, §15) ----
        BigDecimal loanRepayments = BigDecimal.ZERO;
        for (EmployeeLoan loan : employeeLoanRepository.findByEmployeeIdAndStatus(employee.getId(), LoanStatus.ACTIVE)) {
            BigDecimal installment = loan.getInstallmentAmount().min(loan.getOutstandingPrincipal().add(loan.getOutstandingInterest()));
            if (installment.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            loanRepayments = loanRepayments.add(installment);
            PayrollRecordComponent comp = component(null, "Loan Repayment", PayComponentSide.EMPLOYEE_DEDUCTION, installment);
            comp.setDescription("EMPLOYEE_LOAN:" + loan.getId());
            components.add(comp);
        }
        BigDecimal advanceRepayments = BigDecimal.ZERO;
        for (SalaryAdvance advance : salaryAdvanceRepository.findByEmployeeIdAndStatusIn(
                employee.getId(), List.of(AdvanceStatus.OUTSTANDING, AdvanceStatus.PARTIALLY_RECOVERED))) {
            BigDecimal deduction = (advance.getRecurringDeductionAmount() != null
                    ? advance.getRecurringDeductionAmount() : advance.getOutstandingBalance())
                    .min(advance.getOutstandingBalance());
            if (deduction.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            advanceRepayments = advanceRepayments.add(deduction);
            PayrollRecordComponent comp = component(null, "Salary Advance Repayment", PayComponentSide.EMPLOYEE_DEDUCTION, deduction);
            comp.setDescription("SALARY_ADVANCE:" + advance.getId());
            components.add(comp);
        }

        // ---- 9. Totals ----
        BigDecimal totalEmployeeDeductions = structuralDeductions.add(variableDeductions)
                .add(employeeStatutory).add(incomeTax).add(loanRepayments).add(advanceRepayments);
        BigDecimal netPay = grossPay.subtract(totalEmployeeDeductions);
        boolean negativeFlagged = netPay.compareTo(BigDecimal.ZERO) < 0;
        if (negativeFlagged) {
            netPay = BigDecimal.ZERO; // §57 - never post a negative net pay; flag it for human review instead.
        }
        BigDecimal totalEmployerCost = grossPay.add(employerStatutoryExpense);

        EmployeePayrollRecord record = new EmployeePayrollRecord();
        record.setPayrollRun(run);
        record.setEmployee(employee);
        record.setEmployeeSalaryStructure(assignment);
        record.setGrossPay(grossPay.setScale(2, RoundingMode.HALF_UP));
        record.setTotalEmployeeDeductions(totalEmployeeDeductions.setScale(2, RoundingMode.HALF_UP));
        record.setNetPay(netPay.setScale(2, RoundingMode.HALF_UP));
        record.setTotalEmployerCost(totalEmployerCost.setScale(2, RoundingMode.HALF_UP));
        record.setNegativeNetPayFlagged(negativeFlagged);
        components.forEach(c -> c.setEmployeePayrollRecord(record));
        record.setComponents(components);

        return employeePayrollRecordRepository.save(record);
    }

    private BigDecimal resolveLineAmount(SalaryStructureLine line, BigDecimal basicSalary) {
        PayComponent component = line.getPayComponent();
        if (component.getCalculationMethod() == PayComponentCalculationMethod.PERCENTAGE_OF_BASIC) {
            return basicSalary.multiply(line.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return line.getValue();
    }

    private boolean isTaxablePensionable(PayComponent component, boolean defaultValue) {
        return component == null ? defaultValue : component.isPensionable();
    }

    private PayrollRecordComponent component(PayComponent payComponent, String name, PayComponentSide side, BigDecimal amount) {
        PayrollRecordComponent c = new PayrollRecordComponent();
        c.setPayComponent(payComponent);
        c.setComponentName(name);
        c.setSide(side);
        c.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        return c;
    }

    private PayrollRecordComponent statutoryComponent(StatutoryScheme scheme, String name, PayComponentSide side, BigDecimal amount) {
        PayrollRecordComponent c = new PayrollRecordComponent();
        c.setStatutoryScheme(scheme);
        c.setComponentName(name);
        c.setSide(side);
        c.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        return c;
    }
}
