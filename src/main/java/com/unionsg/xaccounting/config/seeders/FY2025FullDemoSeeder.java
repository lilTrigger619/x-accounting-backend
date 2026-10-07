package com.unionsg.xaccounting.config.seeders;

import com.unionsg.xaccounting.dto.accounting.AccountingPeriodResponse;
import com.unionsg.xaccounting.dto.banking.SaveBankTransferRequest;
import com.unionsg.xaccounting.dto.bankrec.CreateAdjustmentRequest;
import com.unionsg.xaccounting.dto.bankrec.SaveReconciliationRequest;
import com.unionsg.xaccounting.dto.deposit.CreateDepositRequest;
import com.unionsg.xaccounting.dto.deposit.ForfeitDepositRequest;
import com.unionsg.xaccounting.dto.deposit.RefundDepositRequest;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentRequest;
import com.unionsg.xaccounting.dto.expense.SaveExpenseLineRequest;
import com.unionsg.xaccounting.dto.expense.SaveExpenseRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalLineRequest;
import com.unionsg.xaccounting.dto.journal.CreateJournalRequest;
import com.unionsg.xaccounting.dto.loan.CreateLoanRequest;
import com.unionsg.xaccounting.dto.loan.LoanActionRequest;
import com.unionsg.xaccounting.dto.loan.LoanLineResponse;
import com.unionsg.xaccounting.dto.loan.LoanResponse;
import com.unionsg.xaccounting.dto.loan.RecordLoanRepaymentRequest;
import com.unionsg.xaccounting.dto.payroll.CreatePayrollCalendarPeriodRequest;
import com.unionsg.xaccounting.dto.payroll.CreatePayrollRunRequest;
import com.unionsg.xaccounting.dto.payroll.PayrollCalendarPeriodResponse;
import com.unionsg.xaccounting.dto.payroll.PayrollRunResponse;
import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentRequest;
import com.unionsg.xaccounting.entity.accounting.FinancialYear;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.deposit.DepositType;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.AccountingPeriodStatus;
import com.unionsg.xaccounting.enums.JournalType;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.bankrec.ReconciliationAdjustmentType;
import com.unionsg.xaccounting.enums.deposit.DepositCounterpartyType;
import com.unionsg.xaccounting.enums.deposit.DepositDirection;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import com.unionsg.xaccounting.enums.loan.LoanCounterpartyType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentCounterpartyType;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentFrequency;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.accounting.FinancialYearRepository;
import com.unionsg.xaccounting.repository.deposit.DepositTypeRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepository;
import com.unionsg.xaccounting.repository.loan.LoanTypeRepository;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentTypeRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.accounting.AccountingPeriodService;
import com.unionsg.xaccounting.service.accounting.YearEndClosingService;
import com.unionsg.xaccounting.service.banking.BankTransferService;
import com.unionsg.xaccounting.service.bankrec.BankReconciliationService;
import com.unionsg.xaccounting.service.deposit.DepositService;
import com.unionsg.xaccounting.service.downpayment.DownpaymentService;
import com.unionsg.xaccounting.service.expense.ExpenseService;
import com.unionsg.xaccounting.service.journal.JournalService;
import com.unionsg.xaccounting.service.loan.LoanService;
import com.unionsg.xaccounting.service.payroll.PayrollCalendarService;
import com.unionsg.xaccounting.service.payroll.PayrollGroupService;
import com.unionsg.xaccounting.service.payroll.PayrollRunService;
import com.unionsg.xaccounting.service.prepayment.PrepaymentService;
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
 * Gives FY 2025 - until now only Invoices/Bills/Payments from {@link DemoDataSeeder} - a second,
 * genuinely complete year: a full twelve-run Payroll year, two Prepayments, two Loans (one
 * borrowed, one lent), and the five newer modules {@link NewModulesDemoSeeder} already seeded
 * for FY 2026. With this, every report and the Business Intelligence screens have two comparable
 * years instead of one thin one.
 *
 * <p>FY 2025 was already closed and fully locked by {@link DemoDataSeeder}, so this seeder first
 * reopens the year and unlocks every one of its periods, posts everything below dated across
 * 2025, then closes the year again - re-running {@link YearEndClosingService#closeFinancialYear}
 * naturally zeroes out only the activity posted since the first closing, since the prior closing
 * already drove every income/expense account to zero for the year.</p>
 *
 * <p>Reuses the org structure, pay components, statutory scheme, tax configuration, salary
 * structure and employees {@link PayrollDemoSeeder} already created (effective from 2025-01-01)
 * rather than duplicating them, and the two KES bank accounts {@link NewModulesDemoSeeder}
 * opened, rather than touching the original USD "Operating Account". Runs last among the
 * seeders, after everything it depends on. Idempotent and transactional, matching the rest of
 * this package.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(9)
public class FY2025FullDemoSeeder implements ApplicationRunner {

    private static final String FY_2025 = "FY 2025";
    private static final String KES_OPERATING_ACCOUNT = "KES Operating Account";
    private static final String KES_RESERVE_ACCOUNT = "KES Reserve Account";
    private static final String MONTHLY_PAYROLL_GROUP = "Monthly Salaried Staff";

    private final LoanRepository loanRepository;
    private final FinancialYearRepository financialYearRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final AccountRepository accountRepository;
    private final DepositTypeRepository depositTypeRepository;
    private final PrepaymentTypeRepository prepaymentTypeRepository;
    private final LoanTypeRepository loanTypeRepository;

    private final YearEndClosingService yearEndClosingService;
    private final AccountingPeriodService accountingPeriodService;
    private final JournalService journalService;
    private final PayrollGroupService payrollGroupService;
    private final PayrollCalendarService payrollCalendarService;
    private final PayrollRunService payrollRunService;
    private final PrepaymentService prepaymentService;
    private final LoanService loanService;
    private final ExpenseService expenseService;
    private final DepositService depositService;
    private final DownpaymentService downpaymentService;
    private final BankTransferService bankTransferService;
    private final BankReconciliationService bankReconciliationService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (loanRepository.count() > 0) {
            log.info("FY 2025 full demo data already present - skipping FY2025FullDemoSeeder.");
            return;
        }

        FinancialYear fy2025 = financialYearRepository.findAll().stream()
                .filter(fy -> FY_2025.equals(fy.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(FY_2025 + " not found - DemoDataSeeder should have seeded it already"));
        Long fy2025Id = fy2025.getId();

        reopenAndUnlock(fy2025Id);

        Long operatingAccountId = bankAccountId(KES_OPERATING_ACCOUNT);
        Long reserveAccountId = bankAccountId(KES_RESERVE_ACCOUNT);
        fundFy2025Accounts();

        seedPayrollYear();
        seedPrepayments(operatingAccountId);
        seedLoans(operatingAccountId, reserveAccountId);
        seedExpenses(operatingAccountId);
        seedDeposits(operatingAccountId);
        seedDownpayments(operatingAccountId);
        seedBankTransfers(operatingAccountId, reserveAccountId);
        seedBankReconciliation(reserveAccountId);

        yearEndClosingService.closeFinancialYear(fy2025Id);
        log.info("FY 2025 full demo seeding complete: re-closed with a full payroll year, prepayments, "
                + "loans and the newer modules all posted alongside the original invoices/bills.");
    }

    // ================= Reopen / unlock =================

    private void reopenAndUnlock(Long financialYearId) {
        yearEndClosingService.reopenFinancialYear(financialYearId,
                "Reopening to seed a full second year of demo activity (payroll, prepayments, loans, "
                        + "and the newer modules) for year-over-year reporting.");

        List<AccountingPeriodResponse> periods = accountingPeriodService.getByFinancialYear(financialYearId);
        for (AccountingPeriodResponse period : periods) {
            if (period.getStatus() == AccountingPeriodStatus.LOCKED) {
                accountingPeriodService.unlock(period.getId(), "Reopened to seed a full FY 2025 demo year");
            }
        }
        log.info("Reopened FY 2025 and unlocked all {} of its accounting periods.", periods.size());
    }

    // ================= Opening funding =================

    private void fundFy2025Accounts() {
        CreateJournalLineRequest debitOperating = CreateJournalLineRequest.builder()
                .accountId(Long.valueOf("1020"))
                .description("Initial 2025 capital contribution to the KES operating account")
                .debitAmount(new BigDecimal("15000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();
        CreateJournalLineRequest debitReserve = CreateJournalLineRequest.builder()
                .accountId(Long.valueOf("1030"))
                .description("Initial 2025 capital contribution to the KES reserve account")
                .debitAmount(new BigDecimal("8000.00"))
                .creditAmount(BigDecimal.ZERO)
                .build();
        CreateJournalLineRequest credit = CreateJournalLineRequest.builder()
                .accountId(Long.valueOf("3030"))
                .description("Capital contributed in 2025 to open the KES accounts")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(new BigDecimal("23000.00"))
                .build();

        CreateJournalRequest request = CreateJournalRequest.builder()
                .journalDate(LocalDate.of(2025, 1, 1))
                .reference("CAP-KES-ACCOUNTS-2025")
                .description("2025 capital contribution funding the KES operating and reserve accounts")
                .journalType(JournalType.GENERAL)
                .lines(List.of(debitOperating, debitReserve, credit))
                .build();

        var created = journalService.create(request);
        journalService.post(created.getId());
        log.info("Funded the two KES bank accounts for FY 2025 with a 23,000 capital contribution dated 2025-01-01.");
    }

    // ================= Payroll: a full twelve-run year =================

    private void seedPayrollYear() {
        Long monthlyGroupId = payrollGroupService.getAll().stream()
                .filter(g -> MONTHLY_PAYROLL_GROUP.equals(g.getName()))
                .map(g -> g.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Payroll group not found: " + MONTHLY_PAYROLL_GROUP
                        + " - PayrollDemoSeeder should have seeded it already"));

        int runsCompleted = 0;
        BigDecimal totalNetPay = BigDecimal.ZERO;
        for (int month = 1; month <= 12; month++) {
            LocalDate periodStart = LocalDate.of(2025, month, 1);
            LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
            LocalDate payDate = periodStart.withDayOfMonth(Math.min(25, periodStart.lengthOfMonth()));

            CreatePayrollCalendarPeriodRequest periodRequest = new CreatePayrollCalendarPeriodRequest();
            periodRequest.setPayrollGroupId(monthlyGroupId);
            periodRequest.setPeriodStart(periodStart);
            periodRequest.setPeriodEnd(periodEnd);
            periodRequest.setPayDate(payDate);
            periodRequest.setAccountingDate(periodEnd);
            PayrollCalendarPeriodResponse period = payrollCalendarService.create(periodRequest);

            CreatePayrollRunRequest runRequest = new CreatePayrollRunRequest();
            runRequest.setPayrollCalendarPeriodId(period.getId());
            PayrollRunResponse run = payrollRunService.create(runRequest);
            payrollRunService.calculate(run.getId());
            payrollRunService.submitForReview(run.getId());
            payrollRunService.approve(run.getId());
            payrollRunService.post(run.getId());
            PayrollRunResponse paid = payrollRunService.pay(run.getId(), payDate);

            runsCompleted++;
            totalNetPay = totalNetPay.add(paid.getTotalNetPay());
        }

        log.info("Seeded a full FY 2025 payroll year: {} monthly runs calculated, approved, posted and paid, "
                + "total net pay {}.", runsCompleted, totalNetPay);
    }

    // ================= Prepayments =================

    private void seedPrepayments(Long bankAccountId) {
        Long insuranceType = prepaymentTypeId("Prepaid Insurance");
        Long subscriptionType = prepaymentTypeId("Software Subscription");
        Supplier keystoneCloud = findSupplier("Keystone Cloud Hosting");

        // A: a full twelve-month prepaid insurance policy, fully recognized across the year.
        CreatePrepaymentRequest insurance = new CreatePrepaymentRequest();
        insurance.setPrepaymentTypeId(insuranceType);
        insurance.setCounterpartyType(PrepaymentCounterpartyType.OTHER);
        insurance.setCounterpartyName("Meridian Assurance Co.");
        insurance.setTotalAmount(new BigDecimal("12000.00"));
        insurance.setCurrency("KES");
        insurance.setPaymentDate(LocalDate.of(2025, 1, 1));
        insurance.setRecognitionStartDate(LocalDate.of(2025, 1, 1));
        insurance.setNumberOfPeriods(12);
        insurance.setRecognitionFrequency(PrepaymentFrequency.MONTHLY);
        insurance.setBankAccountId(bankAccountId);
        insurance.setNotes("Annual general liability policy, paid up front");
        var insurancePrepayment = prepaymentService.create(insurance);
        prepaymentService.activate(insurancePrepayment.getId());
        for (int i = 0; i < 12; i++) {
            prepaymentService.recognizeNext(insurancePrepayment.getId());
        }

        // B: a six-month software subscription, only partially recognized by year end.
        CreatePrepaymentRequest subscription = new CreatePrepaymentRequest();
        subscription.setPrepaymentTypeId(subscriptionType);
        subscription.setCounterpartyType(PrepaymentCounterpartyType.SUPPLIER);
        subscription.setSupplierId(keystoneCloud.getId());
        subscription.setTotalAmount(new BigDecimal("2400.00"));
        subscription.setCurrency("KES");
        subscription.setPaymentDate(LocalDate.of(2025, 6, 1));
        subscription.setRecognitionStartDate(LocalDate.of(2025, 6, 1));
        subscription.setNumberOfPeriods(6);
        subscription.setRecognitionFrequency(PrepaymentFrequency.MONTHLY);
        subscription.setBankAccountId(bankAccountId);
        subscription.setNotes("Six-month cloud hosting subscription, prepaid");
        var subscriptionPrepayment = prepaymentService.create(subscription);
        prepaymentService.activate(subscriptionPrepayment.getId());
        for (int i = 0; i < 4; i++) {
            prepaymentService.recognizeNext(subscriptionPrepayment.getId());
        }

        log.info("Seeded 2 prepayments for FY 2025: a 12-month insurance policy fully recognized, "
                + "and a 6-month subscription partially recognized (4 of 6) at year end.");
    }

    // ================= Loans =================

    private void seedLoans(Long operatingAccountId, Long reserveAccountId) {
        Long bankLoanType = loanTypeId("Bank Loan");
        Long customerLoanType = loanTypeId("Customer Loan");
        Customer solsticeMedia = findCustomer("Solstice Media Co.");

        // A: borrowed, reducing-balance, only partially repaid by year end (stays outstanding).
        CreateLoanRequest borrowed = new CreateLoanRequest();
        borrowed.setLoanTypeId(bankLoanType);
        borrowed.setDirection(LoanDirection.BORROWED_LOAN);
        borrowed.setCounterpartyType(LoanCounterpartyType.BANK);
        borrowed.setCounterpartyName("Meridian Capital Bank");
        borrowed.setPrincipalAmount(new BigDecimal("15000.00"));
        borrowed.setCurrency("KES");
        borrowed.setInterestRate(new BigDecimal("12"));
        borrowed.setInterestMethod(LoanInterestMethod.REDUCING_BALANCE);
        borrowed.setStartDate(LocalDate.of(2025, 2, 1));
        borrowed.setPaymentFrequency(LoanFrequency.MONTHLY);
        borrowed.setNumberOfInstallments(10);
        borrowed.setBankAccountId(operatingAccountId);
        borrowed.setNotes("Working capital bank loan");
        LoanResponse borrowedLoan = loanService.create(borrowed);
        loanService.approve(borrowedLoan.getId());
        LoanActionRequest disburseBorrowed = new LoanActionRequest();
        disburseBorrowed.setDate(LocalDate.of(2025, 2, 1));
        loanService.disburse(borrowedLoan.getId(), disburseBorrowed);

        List<LoanLineResponse> borrowedSchedule = loanService.getSchedule(borrowedLoan.getId());
        for (int i = 0; i < 8 && i < borrowedSchedule.size(); i++) {
            payInstallment(borrowedLoan.getId(), borrowedSchedule.get(i), operatingAccountId);
        }

        // B: lent to a customer, simple interest, repaid in full and closed before year end.
        CreateLoanRequest lent = new CreateLoanRequest();
        lent.setLoanTypeId(customerLoanType);
        lent.setDirection(LoanDirection.LENT_LOAN);
        lent.setCounterpartyType(LoanCounterpartyType.CUSTOMER);
        lent.setCustomerId(solsticeMedia.getId());
        lent.setPrincipalAmount(new BigDecimal("8000.00"));
        lent.setCurrency("KES");
        lent.setInterestRate(new BigDecimal("10"));
        lent.setInterestMethod(LoanInterestMethod.SIMPLE);
        lent.setStartDate(LocalDate.of(2025, 3, 1));
        lent.setPaymentFrequency(LoanFrequency.MONTHLY);
        lent.setNumberOfInstallments(8);
        lent.setBankAccountId(reserveAccountId);
        lent.setNotes("Short-term loan advanced to a customer");
        LoanResponse lentLoan = loanService.create(lent);
        loanService.approve(lentLoan.getId());
        LoanActionRequest disburseLent = new LoanActionRequest();
        disburseLent.setDate(LocalDate.of(2025, 3, 1));
        loanService.disburse(lentLoan.getId(), disburseLent);

        List<LoanLineResponse> lentSchedule = loanService.getSchedule(lentLoan.getId());
        for (LoanLineResponse installment : lentSchedule) {
            payInstallment(lentLoan.getId(), installment, reserveAccountId);
        }
        LoanActionRequest closeLent = new LoanActionRequest();
        closeLent.setReason("Loan fully repaid ahead of schedule");
        loanService.close(lentLoan.getId(), closeLent);

        log.info("Seeded 2 loans for FY 2025: a borrowed bank loan 8 of 10 installments paid (outstanding "
                + "at year end), and a customer loan fully repaid and closed.");
    }

    private void payInstallment(Long loanId, LoanLineResponse installment, Long bankAccountId) {
        RecordLoanRepaymentRequest repayment = new RecordLoanRepaymentRequest();
        repayment.setRepaymentDate(installment.getDueDate());
        repayment.setAmount(installment.getTotalInstallment());
        repayment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        repayment.setBankAccountId(bankAccountId);
        loanService.recordRepayment(loanId, repayment);
    }

    // ================= Expenses =================

    private void seedExpenses(Long paymentAccountId) {
        Supplier meridianFreight = findSupplier("Meridian Freight & Logistics");
        Supplier falconRentals = findSupplier("Falcon Equipment Rentals");
        Long officeSupplies = accountId("5000");
        Long otherExpense = accountId("5020");
        Long shipping = accountId("5030");

        post(expenseService.create(expense(paymentAccountId, null, LocalDate.of(2025, 2, 10), PaymentMethod.CARD,
                "Office stationery restock",
                List.of(line("OFF", officeSupplies, "Office stationery restock", "198.25")))));

        post(expenseService.create(expense(paymentAccountId, idOf(meridianFreight), LocalDate.of(2025, 4, 18), PaymentMethod.BANK_TRANSFER,
                "Quarterly freight charges",
                List.of(line("UTL", shipping, "Quarterly freight charges", "560.00")))));

        post(expenseService.create(expense(paymentAccountId, null, LocalDate.of(2025, 6, 9), PaymentMethod.CASH,
                "Conference registration and travel",
                List.of(
                        line("TRV", otherExpense, "Conference travel", "275.00"),
                        line("OFF", officeSupplies, "Conference materials printing", "120.00")))));

        post(expenseService.create(expense(paymentAccountId, idOf(falconRentals), LocalDate.of(2025, 8, 22), PaymentMethod.CARD,
                "Equipment rental for client install",
                List.of(line("OFF", officeSupplies, "Equipment rental for client install", "430.50")))));

        post(expenseService.create(expense(paymentAccountId, null, LocalDate.of(2025, 10, 5), PaymentMethod.MOBILE_MONEY,
                "Year-end client appreciation gifts",
                List.of(line("MKT", officeSupplies, "Year-end client appreciation gifts", "340.00")))));

        post(expenseService.create(expense(paymentAccountId, null, LocalDate.of(2025, 11, 28), PaymentMethod.CASH,
                "Courier charges",
                List.of(line("UTL", shipping, "Courier charges", "95.75")))));

        log.info("Seeded 6 expenses across FY 2025, one split across two categories.");
    }

    private void post(com.unionsg.xaccounting.dto.expense.ExpenseResponse draft) {
        expenseService.post(draft.getId());
    }

    private SaveExpenseRequest expense(Long paymentAccountId, Long supplierId, LocalDate paymentDate,
                                        PaymentMethod method, String memo, List<SaveExpenseLineRequest> lines) {
        return SaveExpenseRequest.builder()
                .supplierId(supplierId)
                .paymentAccountId(paymentAccountId)
                .paymentDate(paymentDate)
                .paymentMethod(method)
                .memo(memo)
                .lines(lines)
                .build();
    }

    private SaveExpenseLineRequest line(String category, Long accountId, String description, String amount) {
        return SaveExpenseLineRequest.builder()
                .category(category)
                .accountId(accountId)
                .description(description)
                .amount(new BigDecimal(amount))
                .build();
    }

    // ================= Deposits =================

    private void seedDeposits(Long bankAccountId) {
        Long securityDepositType = depositTypeId("Security Deposit");
        Long rentDepositType = depositTypeId("Rent Deposit");
        Long customerAdvanceType = depositTypeId("Customer Advance Deposit");
        Long equipmentDepositType = depositTypeId("Equipment / Container Deposit");
        Customer harborFinch = findCustomer("Harbor & Finch Consulting");

        // A: paid, held, never touched again.
        depositService.create(depositPaid(rentDepositType, "Greenview Office Park (Landlord)",
                "2200.00", bankAccountId, LocalDate.of(2025, 1, 15)), true);

        // B: paid, then refunded in full.
        var securityDeposit = depositService.create(depositPaid(securityDepositType, "Falcon Equipment Rentals",
                "800.00", bankAccountId, LocalDate.of(2025, 2, 20)), true);
        RefundDepositRequest refund = new RefundDepositRequest();
        refund.setAmount(new BigDecimal("800.00"));
        refund.setRefundDate(LocalDate.of(2025, 9, 10));
        refund.setNotes("Rental contract ended; equipment returned undamaged");
        depositService.refund(securityDeposit.getId(), refund);

        // C: received from a customer, then partially forfeited.
        var customerDeposit = depositService.create(depositReceived(customerAdvanceType,
                DepositCounterpartyType.CUSTOMER, harborFinch.getId(), null,
                "1800.00", bankAccountId, LocalDate.of(2025, 5, 5)), true);
        ForfeitDepositRequest forfeit = new ForfeitDepositRequest();
        forfeit.setAmount(new BigDecimal("300.00"));
        forfeit.setForfeitDate(LocalDate.of(2025, 10, 30));
        forfeit.setNotes("Order cancelled after work had begun");
        depositService.forfeit(customerDeposit.getId(), forfeit);

        // D: received, held, never touched again.
        depositService.create(depositReceived(equipmentDepositType, DepositCounterpartyType.OTHER, null,
                "Unit 12 Container Lease", "1200.00", bankAccountId, LocalDate.of(2025, 7, 12)), true);

        log.info("Seeded 4 deposits for FY 2025 (2 paid, 2 received) - one refunded in full, one partially forfeited.");
    }

    private CreateDepositRequest depositPaid(Long typeId, String counterpartyName, String amount,
                                              Long bankAccountId, LocalDate depositDate) {
        CreateDepositRequest r = new CreateDepositRequest();
        r.setDirection(DepositDirection.DEPOSIT_PAID);
        r.setDepositTypeId(typeId);
        r.setCounterpartyType(DepositCounterpartyType.OTHER);
        r.setCounterpartyName(counterpartyName);
        r.setAmount(new BigDecimal(amount));
        r.setCurrency("KES");
        r.setDepositDate(depositDate);
        r.setBankAccountId(bankAccountId);
        return r;
    }

    private CreateDepositRequest depositReceived(Long typeId, DepositCounterpartyType counterpartyType,
                                                  Long customerId, String counterpartyName, String amount,
                                                  Long bankAccountId, LocalDate depositDate) {
        CreateDepositRequest r = new CreateDepositRequest();
        r.setDirection(DepositDirection.DEPOSIT_RECEIVED);
        r.setDepositTypeId(typeId);
        r.setCounterpartyType(counterpartyType);
        r.setCustomerId(customerId);
        r.setCounterpartyName(counterpartyName);
        r.setAmount(new BigDecimal(amount));
        r.setCurrency("KES");
        r.setDepositDate(depositDate);
        r.setBankAccountId(bankAccountId);
        return r;
    }

    private Long depositTypeId(String name) {
        return depositTypeRepository.findByDeletedFalse().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .map(DepositType::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Deposit type not found: " + name));
    }

    // ================= Down Payments =================

    private void seedDownpayments(Long bankAccountId) {
        Customer cedarwood = findCustomer("Cedarwood Interiors");
        Supplier voltageUtilities = findSupplier("Voltage Utilities Inc.");

        DownpaymentRequest customerDp = new DownpaymentRequest();
        customerDp.setType(DownpaymentType.CUSTOMER_DOWNPAYMENT);
        customerDp.setCustomerId(cedarwood.getId());
        customerDp.setAmount(new BigDecimal("4200.00"));
        customerDp.setCurrency("KES");
        customerDp.setBankAccountId(bankAccountId);
        customerDp.setPaymentDate(LocalDate.of(2025, 3, 18));
        customerDp.setDescription("Advance against a custom interiors order, agreed before invoicing");
        customerDp.setPost(true);
        downpaymentService.create(customerDp);

        DownpaymentRequest supplierDp = new DownpaymentRequest();
        supplierDp.setType(DownpaymentType.SUPPLIER_DOWNPAYMENT);
        supplierDp.setSupplierId(voltageUtilities.getId());
        supplierDp.setAmount(new BigDecimal("1500.00"));
        supplierDp.setCurrency("KES");
        supplierDp.setBankAccountId(bankAccountId);
        supplierDp.setPaymentDate(LocalDate.of(2025, 9, 2));
        supplierDp.setDescription("Advance against an upcoming infrastructure upgrade");
        supplierDp.setPost(true);
        downpaymentService.create(supplierDp);

        log.info("Seeded 1 customer downpayment and 1 supplier downpayment for FY 2025, both posted and open.");
    }

    // ================= Bank Transfers =================

    private void seedBankTransfers(Long operatingAccountId, Long reserveAccountId) {
        var t1 = bankTransferService.create(transfer(operatingAccountId, reserveAccountId,
                LocalDate.of(2025, 2, 15), "4000.00", null, null));
        bankTransferService.post(t1.getId());

        var t2 = bankTransferService.create(transfer(reserveAccountId, operatingAccountId,
                LocalDate.of(2025, 6, 10), "1500.00", "12.00", null));
        bankTransferService.post(t2.getId());

        var t3 = bankTransferService.create(transfer(operatingAccountId, reserveAccountId,
                LocalDate.of(2025, 11, 1), "2500.00", null, null));
        bankTransferService.post(t3.getId());

        log.info("Seeded 3 bank transfers between the two KES accounts for FY 2025, one carrying a transfer fee.");
    }

    private SaveBankTransferRequest transfer(Long sourceId, Long destinationId, LocalDate date,
                                              String amount, String feeAmount, String feeAccountCode) {
        return SaveBankTransferRequest.builder()
                .sourceBankAccountId(sourceId)
                .destinationBankAccountId(destinationId)
                .transferDate(date)
                .amount(new BigDecimal(amount))
                .feeAmount(feeAmount != null ? new BigDecimal(feeAmount) : null)
                .feeAccountCode(feeAccountCode)
                .description("Routine transfer between operating and reserve accounts")
                .build();
    }

    // ================= Bank Reconciliation =================

    private void seedBankReconciliation(Long reserveAccountId) {
        // Uses the reserve account rather than the operating account - NewModulesDemoSeeder left a
        // reconciliation open (never completed) on the operating account for March 2026, and only
        // one reconciliation can be open per bank account at a time.
        SaveReconciliationRequest request = SaveReconciliationRequest.builder()
                .bankAccountId(reserveAccountId)
                .statementDate(LocalDate.of(2025, 9, 30))
                .periodStart(LocalDate.of(2025, 9, 1))
                .periodEnd(LocalDate.of(2025, 9, 30))
                .openingBankBalance(BigDecimal.ZERO)
                .closingBankBalance(BigDecimal.ZERO)
                .tolerance(new BigDecimal("50.00"))
                .notes("September 2025 statement for the KES reserve account")
                .build();
        var reconciliation = bankReconciliationService.create(request);

        bankReconciliationService.createAdjustment(reconciliation.getId(), CreateAdjustmentRequest.builder()
                .adjustmentType(ReconciliationAdjustmentType.BANK_CHARGE)
                .transactionDate(LocalDate.of(2025, 9, 30))
                .amount(new BigDecimal("18.00"))
                .description("Monthly account maintenance fee")
                .build());

        bankReconciliationService.createAdjustment(reconciliation.getId(), CreateAdjustmentRequest.builder()
                .adjustmentType(ReconciliationAdjustmentType.BANK_INTEREST)
                .transactionDate(LocalDate.of(2025, 9, 30))
                .amount(new BigDecimal("9.25"))
                .description("Interest earned on balance")
                .build());

        log.info("Seeded one in-progress bank reconciliation for September 2025 with a bank charge and "
                + "a bank interest adjustment posted.");
    }

    // ================= Shared helpers =================

    private Long bankAccountId(String accountName) {
        return bankAccountRepository.findAll().stream()
                .filter(a -> accountName.equals(a.getAccountName()))
                .map(BankAccount::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Bank account not found: " + accountName
                        + " - NewModulesDemoSeeder should have seeded it already"));
    }

    private Long accountId(String code) {
        return accountRepository.findByAccountId(code)
                .orElseThrow(() -> new IllegalStateException("Chart of accounts entry not found: " + code))
                .getId();
    }

    private Long prepaymentTypeId(String name) {
        return prepaymentTypeRepository.findByDeletedFalse().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .map(com.unionsg.xaccounting.entity.prepayment.PrepaymentType::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Prepayment type not found: " + name));
    }

    private Long loanTypeId(String name) {
        return loanTypeRepository.findByDeletedFalse().stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .map(com.unionsg.xaccounting.entity.loan.LoanType::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Loan type not found: " + name));
    }

    private Customer findCustomer(String displayName) {
        return customerRepository.findAll().stream()
                .filter(c -> displayName.equals(c.getDisplayName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo customer not found: " + displayName));
    }

    private Supplier findSupplier(String displayName) {
        return supplierRepository.findAll().stream()
                .filter(s -> displayName.equals(s.getDisplayName()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Demo supplier not found: " + displayName));
    }

    private Long idOf(Supplier supplier) {
        return supplier != null ? supplier.getId() : null;
    }
}
