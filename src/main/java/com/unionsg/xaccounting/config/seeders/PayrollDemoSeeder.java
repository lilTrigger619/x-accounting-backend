package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.dto.payroll.ChangeCompensationRequest;
import com.unionsg.xaccounting.dto.payroll.CreateDepartmentRequest;
import com.unionsg.xaccounting.dto.payroll.CreateEmployeeLoanRequest;
import com.unionsg.xaccounting.dto.payroll.CreateEmployeeRequest;
import com.unionsg.xaccounting.dto.payroll.CreatePayComponentRequest;
import com.unionsg.xaccounting.dto.payroll.CreatePayrollCalendarPeriodRequest;
import com.unionsg.xaccounting.dto.payroll.CreatePayrollGroupRequest;
import com.unionsg.xaccounting.dto.payroll.CreatePayrollInputRequest;
import com.unionsg.xaccounting.dto.payroll.CreatePayrollRunRequest;
import com.unionsg.xaccounting.dto.payroll.CreatePositionRequest;
import com.unionsg.xaccounting.dto.payroll.CreateSalaryAdvanceRequest;
import com.unionsg.xaccounting.dto.payroll.CreateSalaryStructureRequest;
import com.unionsg.xaccounting.dto.payroll.CreateStatutorySchemeRequest;
import com.unionsg.xaccounting.dto.payroll.CreateTaxConfigurationRequest;
import com.unionsg.xaccounting.dto.payroll.DepartmentResponse;
import com.unionsg.xaccounting.dto.payroll.EmployeeResponse;
import com.unionsg.xaccounting.dto.payroll.PayComponentResponse;
import com.unionsg.xaccounting.dto.payroll.PayrollCalendarPeriodResponse;
import com.unionsg.xaccounting.dto.payroll.PayrollGroupResponse;
import com.unionsg.xaccounting.dto.payroll.PositionResponse;
import com.unionsg.xaccounting.dto.payroll.SalaryStructureLineRequest;
import com.unionsg.xaccounting.dto.payroll.SalaryStructureResponse;
import com.unionsg.xaccounting.dto.payroll.TaxBracketRequest;
import com.unionsg.xaccounting.enums.EmploymentType;
import com.unionsg.xaccounting.enums.PayComponentCalculationMethod;
import com.unionsg.xaccounting.enums.PayComponentCategory;
import com.unionsg.xaccounting.enums.PayComponentSide;
import com.unionsg.xaccounting.enums.PayFrequency;
import com.unionsg.xaccounting.enums.PayrollInputSourceType;
import com.unionsg.xaccounting.enums.StatutoryCalculationBasis;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.repository.payroll.PayrollRunRepository;
import com.unionsg.xaccounting.service.payroll.DepartmentService;
import com.unionsg.xaccounting.service.payroll.EmployeeLoanService;
import com.unionsg.xaccounting.service.payroll.EmployeeService;
import com.unionsg.xaccounting.service.payroll.PayComponentService;
import com.unionsg.xaccounting.service.payroll.PayrollCalendarService;
import com.unionsg.xaccounting.service.payroll.PayrollGroupService;
import com.unionsg.xaccounting.service.payroll.PayrollInputService;
import com.unionsg.xaccounting.service.payroll.PayrollRunService;
import com.unionsg.xaccounting.service.payroll.PositionService;
import com.unionsg.xaccounting.service.payroll.SalaryAdvanceService;
import com.unionsg.xaccounting.service.payroll.SalaryStructureService;
import com.unionsg.xaccounting.service.payroll.StatutorySchemeService;
import com.unionsg.xaccounting.service.payroll.TaxConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds a complete, realistic Payroll demo: org structure, pay components with real GL mappings,
 * a statutory scheme and a progressive tax configuration, a salary structure, five employees (one
 * with an active loan, one with an outstanding advance), an approved overtime input, and one
 * payroll run driven all the way through calculate -> submit for review -> approve -> post -> pay
 * - so the demo shows the module's central principle (calculation, approval, accounting and
 * payment as separate, auditable events) rather than a single "run payroll" button.
 *
 * <p>Runs after {@link DemoDataSeeder} (which seeds the Financial Years and chart of accounts
 * this depends on) and is itself idempotent and transactional, matching that seeder's pattern.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class PayrollDemoSeeder implements ApplicationRunner {

    private static final String HOUSING_ALLOWANCE = "HOUSING_ALLOW";
    private static final String TRANSPORT_ALLOWANCE = "TRANSPORT_ALLOW";
    private static final String OVERTIME_COMPONENT = "OVERTIME";
    private static final String BONUS_COMPONENT = "BONUS";

    private final FinancialYearRepository financialYearRepository;
    private final PayrollRunRepository payrollRunRepository;

    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final PayrollGroupService payrollGroupService;
    private final PayComponentService payComponentService;
    private final SalaryStructureService salaryStructureService;
    private final StatutorySchemeService statutorySchemeService;
    private final TaxConfigurationService taxConfigurationService;
    private final EmployeeService employeeService;
    private final EmployeeLoanService employeeLoanService;
    private final SalaryAdvanceService salaryAdvanceService;
    private final PayrollCalendarService payrollCalendarService;
    private final PayrollInputService payrollInputService;
    private final PayrollRunService payrollRunService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (payrollRunRepository.count() > 0) {
            log.info("Payroll demo data already present - skipping PayrollDemoSeeder.");
            return;
        }
        if (financialYearRepository.count() == 0) {
            log.warn("No Financial Year found yet - skipping PayrollDemoSeeder until DemoDataSeeder has run.");
            return;
        }

        LocalDate fyStart = LocalDate.of(2025, 1, 1);

        DepartmentResponse finance = departmentService.create(dept("Finance", "CC-100"));
        DepartmentResponse engineering = departmentService.create(dept("Engineering", "CC-200"));
        PositionResponse accountant = positionService.create(position("Senior Accountant", finance.getId()));
        PositionResponse engineer = positionService.create(position("Software Engineer", engineering.getId()));

        PayrollGroupResponse monthlyGroup = payrollGroupService.create(payrollGroup("Monthly Salaried Staff", PayFrequency.MONTHLY));

        PayComponentResponse housing = payComponentService.create(component(HOUSING_ALLOWANCE, "Housing Allowance",
                PayComponentCategory.ALLOWANCE, PayComponentSide.EARNING, PayComponentCalculationMethod.PERCENTAGE_OF_BASIC,
                new BigDecimal("15"), true, false, "5040"));
        PayComponentResponse transport = payComponentService.create(component(TRANSPORT_ALLOWANCE, "Transport Allowance",
                PayComponentCategory.ALLOWANCE, PayComponentSide.EARNING, PayComponentCalculationMethod.FIXED_AMOUNT,
                new BigDecimal("150"), true, false, "5040"));
        payComponentService.create(component(OVERTIME_COMPONENT, "Overtime",
                PayComponentCategory.OVERTIME, PayComponentSide.EARNING, PayComponentCalculationMethod.HOURS_TIMES_RATE,
                null, true, false, "5040"));
        payComponentService.create(component(BONUS_COMPONENT, "Performance Bonus",
                PayComponentCategory.BONUS, PayComponentSide.EARNING, PayComponentCalculationMethod.FIXED_AMOUNT,
                null, true, false, "5040"));

        SalaryStructureResponse structure = salaryStructureService.create(
                salaryStructure("Standard Monthly", List.of(
                        lineOf(housing.getId(), new BigDecimal("15")),
                        lineOf(transport.getId(), new BigDecimal("150"))
                )));

        statutorySchemeService.create(statutoryScheme("PENSION", "National Pension Scheme",
                new BigDecimal("5"), new BigDecimal("8"), "2120", "5050", "2130", fyStart));

        taxConfigurationService.create(taxConfiguration("Standard Progressive Tax", "Demo Jurisdiction", fyStart, "2110",
                List.of(
                        bracket(1, BigDecimal.ZERO, new BigDecimal("2000"), new BigDecimal("10")),
                        bracket(2, new BigDecimal("2000"), new BigDecimal("5000"), new BigDecimal("20")),
                        bracket(3, new BigDecimal("5000"), null, new BigDecimal("30"))
                )));

        EmployeeResponse alice = hireEmployee("Alice", "Nkemah", finance.getId(), accountant.getId(),
                monthlyGroup.getId(), structure.getId(), new BigDecimal("4500"), fyStart);
        EmployeeResponse ben = hireEmployee("Ben", "Osei", engineering.getId(), engineer.getId(),
                monthlyGroup.getId(), structure.getId(), new BigDecimal("5200"), fyStart);
        EmployeeResponse chloe = hireEmployee("Chloe", "Mensah", engineering.getId(), engineer.getId(),
                monthlyGroup.getId(), structure.getId(), new BigDecimal("4800"), fyStart);
        EmployeeResponse david = hireEmployee("David", "Owusu", finance.getId(), accountant.getId(),
                monthlyGroup.getId(), structure.getId(), new BigDecimal("3900"), fyStart);
        EmployeeResponse elena = hireEmployee("Elena", "Boateng", engineering.getId(), engineer.getId(),
                monthlyGroup.getId(), structure.getId(), new BigDecimal("4200"), fyStart);

        // Give one employee a raise later in the year, so compensation history (§6, §59) has more than one row.
        employeeService.changeCompensation(ben.getId(), raise(structure.getId(), new BigDecimal("5600"),
                LocalDate.of(2025, 7, 1), "Mid-year performance increment"));

        // An active loan and an outstanding advance, to exercise §14/§15 through a real run.
        // Dated in FY2026 (not FY2025, which DemoDataSeeder has already closed and fully locked)
        // and after FY2026's first two periods, which DemoDataSeeder locks deliberately (§7.2).
        CreateEmployeeLoanRequest loanRequest = new CreateEmployeeLoanRequest();
        loanRequest.setEmployeeId(chloe.getId());
        loanRequest.setPrincipal(new BigDecimal("1200"));
        loanRequest.setInterestRatePercent(BigDecimal.ZERO);
        loanRequest.setStartDate(LocalDate.of(2026, 4, 1));
        loanRequest.setInstallmentAmount(new BigDecimal("100"));
        loanRequest.setReason("Emergency medical loan");
        employeeLoanService.disburse(loanRequest);

        CreateSalaryAdvanceRequest advanceRequest = new CreateSalaryAdvanceRequest();
        advanceRequest.setEmployeeId(david.getId());
        advanceRequest.setAmountIssued(new BigDecimal("500"));
        advanceRequest.setDateIssued(LocalDate.of(2026, 4, 1));
        advanceRequest.setRecurringDeductionAmount(new BigDecimal("50"));
        advanceRequest.setReason("Requested salary advance");
        salaryAdvanceService.issue(advanceRequest);

        // The payroll period this demo run covers: April 2026 - inside FY2026, and past the
        // first two (locked) periods.
        CreatePayrollCalendarPeriodRequest periodRequest = new CreatePayrollCalendarPeriodRequest();
        periodRequest.setPayrollGroupId(monthlyGroup.getId());
        periodRequest.setPeriodStart(LocalDate.of(2026, 4, 1));
        periodRequest.setPeriodEnd(LocalDate.of(2026, 4, 30));
        periodRequest.setPayDate(LocalDate.of(2026, 5, 5));
        periodRequest.setAccountingDate(LocalDate.of(2026, 4, 30));
        PayrollCalendarPeriodResponse period = payrollCalendarService.create(periodRequest);

        // An approved overtime input for Alice, so the run exercises a variable payroll input (§12, §18).
        CreatePayrollInputRequest overtimeInput = new CreatePayrollInputRequest();
        overtimeInput.setEmployeeId(alice.getId());
        overtimeInput.setPayrollCalendarPeriodId(period.getId());
        overtimeInput.setSourceType(PayrollInputSourceType.OVERTIME);
        overtimeInput.setPayComponentId(payComponentService.getByCode(OVERTIME_COMPONENT).getId());
        overtimeInput.setDescription("June weekend release support");
        overtimeInput.setHours(new BigDecimal("6"));
        overtimeInput.setRate(new BigDecimal("25"));
        overtimeInput.setMultiplier(new BigDecimal("1.5"));
        var savedInput = payrollInputService.create(overtimeInput);
        payrollInputService.approve(savedInput.getId());

        // Drive one full run through the whole lifecycle: calculation, review, approval,
        // accounting posting, and payment are five separate, auditable events (the module's
        // central requirement), not one action.
        var run = payrollRunService.create(runRequest(period.getId()));
        payrollRunService.calculate(run.getId());
        payrollRunService.submitForReview(run.getId());
        payrollRunService.approve(run.getId());
        payrollRunService.post(run.getId());
        payrollRunService.pay(run.getId(), period.getPayDate());

        log.info("Payroll demo seeding complete: run {} posted and paid for {} employees, net pay {}.",
                run.getRunNumber(), 5, payrollRunService.getById(run.getId()).getTotalNetPay());
    }

    // ================= helpers =================

    private CreateDepartmentRequest dept(String name, String costCenter) {
        CreateDepartmentRequest r = new CreateDepartmentRequest();
        r.setName(name);
        r.setCostCenterCode(costCenter);
        return r;
    }

    private CreatePositionRequest position(String title, Long departmentId) {
        CreatePositionRequest r = new CreatePositionRequest();
        r.setTitle(title);
        r.setDepartmentId(departmentId);
        return r;
    }

    private CreatePayrollGroupRequest payrollGroup(String name, PayFrequency frequency) {
        CreatePayrollGroupRequest r = new CreatePayrollGroupRequest();
        r.setName(name);
        r.setPayFrequency(frequency);
        return r;
    }

    private CreatePayComponentRequest component(String code, String name, PayComponentCategory category,
                                                 PayComponentSide side, PayComponentCalculationMethod method,
                                                 BigDecimal defaultValue, boolean taxable, boolean pensionable,
                                                 String glDebitAccountCode) {
        CreatePayComponentRequest r = new CreatePayComponentRequest();
        r.setCode(code);
        r.setName(name);
        r.setCategory(category);
        r.setSide(side);
        r.setCalculationMethod(method);
        r.setDefaultValue(defaultValue);
        r.setTaxable(taxable);
        r.setPensionable(pensionable);
        r.setGlDebitAccountCode(glDebitAccountCode);
        return r;
    }

    private SalaryStructureLineRequest lineOf(Long payComponentId, BigDecimal value) {
        SalaryStructureLineRequest r = new SalaryStructureLineRequest();
        r.setPayComponentId(payComponentId);
        r.setValue(value);
        return r;
    }

    private CreateSalaryStructureRequest salaryStructure(String name, List<SalaryStructureLineRequest> lines) {
        CreateSalaryStructureRequest r = new CreateSalaryStructureRequest();
        r.setName(name);
        r.setLines(lines);
        return r;
    }

    private CreateStatutorySchemeRequest statutoryScheme(String code, String name, BigDecimal employeeRate,
                                                          BigDecimal employerRate, String employeeLiabilityAccount,
                                                          String employerExpenseAccount, String employerLiabilityAccount,
                                                          LocalDate effectiveFrom) {
        CreateStatutorySchemeRequest r = new CreateStatutorySchemeRequest();
        r.setCode(code);
        r.setName(name);
        r.setCalculationBasis(StatutoryCalculationBasis.PENSIONABLE_EARNINGS);
        r.setEmployeeRatePercent(employeeRate);
        r.setEmployerRatePercent(employerRate);
        r.setEmployeeLiabilityAccountCode(employeeLiabilityAccount);
        r.setEmployerExpenseAccountCode(employerExpenseAccount);
        r.setEmployerLiabilityAccountCode(employerLiabilityAccount);
        r.setEffectiveFrom(effectiveFrom);
        return r;
    }

    private TaxBracketRequest bracket(int line, BigDecimal min, BigDecimal max, BigDecimal rate) {
        TaxBracketRequest r = new TaxBracketRequest();
        r.setLineNumber(line);
        r.setMinIncome(min);
        r.setMaxIncome(max);
        r.setRatePercent(rate);
        return r;
    }

    private CreateTaxConfigurationRequest taxConfiguration(String name, String jurisdiction, LocalDate effectiveFrom,
                                                            String taxPayableAccountCode, List<TaxBracketRequest> brackets) {
        CreateTaxConfigurationRequest r = new CreateTaxConfigurationRequest();
        r.setName(name);
        r.setJurisdiction(jurisdiction);
        r.setEffectiveFrom(effectiveFrom);
        r.setTaxPayableAccountCode(taxPayableAccountCode);
        r.setBrackets(brackets);
        return r;
    }

    private EmployeeResponse hireEmployee(String firstName, String lastName, Long departmentId, Long positionId,
                                           Long payrollGroupId, Long salaryStructureId, BigDecimal basicSalary,
                                           LocalDate effectiveFrom) {
        CreateEmployeeRequest r = new CreateEmployeeRequest();
        r.setFirstName(firstName);
        r.setLastName(lastName);
        r.setWorkEmail((firstName + "." + lastName + "@unionaccounting.demo").toLowerCase());
        r.setPhone("+1-555-0100");
        r.setDepartmentId(departmentId);
        r.setPositionId(positionId);
        r.setEmploymentType(EmploymentType.FULL_TIME);
        r.setDateOfEmployment(effectiveFrom);
        r.setPayrollGroupId(payrollGroupId);
        r.setBankName("Union Trust Bank");
        r.setBankAccountName(firstName + " " + lastName);
        r.setBankAccountNumber("ACCT-" + System.nanoTime() % 1_000_000);
        r.setCurrency("USD");
        r.setSalaryStructureId(salaryStructureId);
        r.setBasicSalary(basicSalary);
        r.setCompensationEffectiveFrom(effectiveFrom);
        return employeeService.create(r);
    }

    private ChangeCompensationRequest raise(Long salaryStructureId, BigDecimal newBasicSalary, LocalDate effectiveFrom, String reason) {
        ChangeCompensationRequest r = new ChangeCompensationRequest();
        r.setSalaryStructureId(salaryStructureId);
        r.setBasicSalary(newBasicSalary);
        r.setEffectiveFrom(effectiveFrom);
        r.setReason(reason);
        return r;
    }

    private CreatePayrollRunRequest runRequest(Long payrollCalendarPeriodId) {
        CreatePayrollRunRequest r = new CreatePayrollRunRequest();
        r.setPayrollCalendarPeriodId(payrollCalendarPeriodId);
        return r;
    }
}
