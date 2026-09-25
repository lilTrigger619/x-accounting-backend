package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.MapperLayer.LoanMapper;
import com.unionsg.xaccounting.dto.loan.CreateLoanRequest;
import com.unionsg.xaccounting.dto.loan.LoanListItemResponse;
import com.unionsg.xaccounting.dto.loan.LoanRepaymentResponse;
import com.unionsg.xaccounting.dto.loan.LoanResponse;
import com.unionsg.xaccounting.dto.loan.RecordLoanRepaymentRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.entity.loan.LoanType;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.loan.LoanCounterpartyType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFrequency;
import com.unionsg.xaccounting.enums.loan.LoanInstallmentStatus;
import com.unionsg.xaccounting.enums.loan.LoanRepaymentMethod;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepaymentRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepository;
import com.unionsg.xaccounting.repository.loan.LoanTypeRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Lifecycle for the Loan Management subsystem (Loans spec §12-22): create a draft with its
 * amortization schedule, approve, disburse (posts the GL journal in the direction-specific way
 * {@link LoanJournalService} implements), record repayments against it, and settle/write off.
 *
 * <p>Deliberately out of scope this pass (documented, not silently dropped): restructuring/
 * refinancing with formal debt-modification accounting, automatic overdue-status detection (no
 * scheduler exists in this codebase for anything similar), a distinct balloon-payment formula
 * (treated the same as interest-only), and true per-line editing of a CUSTOM_SCHEDULE loan
 * (generated the same way as EQUAL_INSTALLMENT for now). Fee capitalization/amortization
 * treatments beyond EXPENSED_IMMEDIATELY simply add the fee to the initial principal balance
 * rather than running their own separate amortization.</p>
 */
@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository repository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final DocumentNumberService documentNumberService;
    private final LoanJournalService journalService;

    @Transactional
    public LoanResponse create(CreateLoanRequest request) {
        if (request.getPrincipalAmount() == null || request.getPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Principal amount must be positive");
        }
        if (request.getNumberOfInstallments() == null || request.getNumberOfInstallments() <= 0) {
            throw new BusinessException("Number of installments must be positive");
        }

        LoanType loanType = loanTypeRepository.findById(request.getLoanTypeId())
                .orElseThrow(() -> new BusinessException("Loan type not found with ID: " + request.getLoanTypeId()));

        Loan loan = new Loan();
        loan.setLoanType(loanType);
        loan.setDirection(request.getDirection());
        loan.setCounterpartyType(request.getCounterpartyType());
        resolveCounterparty(loan, request);

        loan.setPrincipalAmount(request.getPrincipalAmount());
        loan.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        loan.setInterestRate(request.getInterestRate() != null ? request.getInterestRate() : BigDecimal.ZERO);
        loan.setInterestType(request.getInterestType());
        loan.setInterestMethod(request.getInterestMethod());
        loan.setStartDate(request.getStartDate());
        loan.setMaturityDate(request.getMaturityDate());
        loan.setPaymentFrequency(request.getPaymentFrequency());
        loan.setNumberOfInstallments(request.getNumberOfInstallments());
        loan.setRepaymentMethod(request.getRepaymentMethod());
        loan.setTotalFees(request.getTotalFees() != null ? request.getTotalFees() : BigDecimal.ZERO);
        loan.setFeeTreatment(request.getFeeTreatment());
        loan.setNotes(request.getNotes());
        loan.setStatus(LoanStatus.DRAFT);
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setOutstandingInterest(BigDecimal.ZERO);

        if (request.getBankAccountId() != null) {
            loan.setBankAccount(bankAccountRepository.findById(request.getBankAccountId())
                    .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + request.getBankAccountId())));
        }
        if (request.getPrincipalAccountId() != null) {
            loan.setPrincipalAccount(resolveAccount(request.getPrincipalAccountId()));
        }
        if (request.getInterestAccountId() != null) {
            loan.setInterestAccount(resolveAccount(request.getInterestAccountId()));
        }

        loan.setLoanNumber(documentNumberService.generateNextNumber(DocumentModule.LOAN));

        generateSchedule(loan);

        return LoanMapper.toResponse(repository.save(loan));
    }

    private void resolveCounterparty(Loan loan, CreateLoanRequest request) {
        LoanCounterpartyType type = request.getCounterpartyType();
        if (type == LoanCounterpartyType.EMPLOYEE) {
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new BusinessException("Employee not found with ID: " + request.getEmployeeId()));
            loan.setEmployee(employee);
            loan.setCounterpartyName(employee.getFullName());
        } else if (type == LoanCounterpartyType.CUSTOMER) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new BusinessException("Customer not found with ID: " + request.getCustomerId()));
            loan.setCustomer(customer);
            loan.setCounterpartyName(customer.getDisplayName());
        } else if (type == LoanCounterpartyType.SUPPLIER) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new BusinessException("Supplier not found with ID: " + request.getSupplierId()));
            loan.setSupplier(supplier);
            loan.setCounterpartyName(supplier.getDisplayName());
        } else {
            if (request.getCounterpartyName() == null || request.getCounterpartyName().isBlank()) {
                throw new BusinessException("A counterparty name is required for this counterparty type");
            }
            loan.setCounterpartyName(request.getCounterpartyName());
        }
    }

    /**
     * Builds the repayment schedule per {@code repaymentMethod} (§17). {@code periodRate} is the
     * loan's annual interest rate converted to a per-installment rate based on
     * {@code paymentFrequency} (e.g. a 12% annual rate on a monthly loan is 1% per installment).
     */
    private void generateSchedule(Loan loan) {
        int n = loan.getNumberOfInstallments();
        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal periodRate = periodRate(loan.getInterestRate(), loan.getPaymentFrequency());
        LoanRepaymentMethod method = loan.getRepaymentMethod();

        List<LoanAmortizationLine> schedule = new ArrayList<>();
        BigDecimal balance = principal;
        LocalDate dueDate = loan.getStartDate();

        BigDecimal equalInstallmentPayment = (method == LoanRepaymentMethod.EQUAL_INSTALLMENT
                || method == LoanRepaymentMethod.CUSTOM_SCHEDULE)
                ? calculateEqualInstallment(principal, periodRate, n)
                : null;
        BigDecimal equalPrincipalPerPeriod = (method == LoanRepaymentMethod.EQUAL_PRINCIPAL)
                ? principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN)
                : null;

        BigDecimal principalAllocated = BigDecimal.ZERO;

        for (int i = 1; i <= n; i++) {
            dueDate = advance(dueDate, loan.getPaymentFrequency());
            BigDecimal opening = balance;
            BigDecimal interestDue = opening.multiply(periodRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalDue;

            boolean isLast = (i == n);
            switch (method) {
                case EQUAL_PRINCIPAL -> principalDue = isLast
                        ? principal.subtract(principalAllocated)
                        : equalPrincipalPerPeriod;
                case INTEREST_ONLY, BALLOON_PAYMENT -> principalDue = isLast ? opening : BigDecimal.ZERO;
                default -> { // EQUAL_INSTALLMENT, CUSTOM_SCHEDULE
                    BigDecimal payment = equalInstallmentPayment;
                    principalDue = isLast ? opening : payment.subtract(interestDue).max(BigDecimal.ZERO);
                }
            }
            principalDue = principalDue.min(opening);
            principalAllocated = principalAllocated.add(principalDue);
            BigDecimal closing = opening.subtract(principalDue);
            BigDecimal total = principalDue.add(interestDue);

            LoanAmortizationLine line = new LoanAmortizationLine();
            line.setLoan(loan);
            line.setInstallmentNumber(i);
            line.setDueDate(dueDate);
            line.setOpeningPrincipal(opening);
            line.setPrincipalDue(principalDue);
            line.setInterestDue(interestDue);
            line.setTotalInstallment(total);
            line.setClosingPrincipal(closing);
            line.setStatus(LoanInstallmentStatus.PENDING);
            schedule.add(line);

            balance = closing;
        }

        loan.setSchedule(schedule);
    }

    private BigDecimal calculateEqualInstallment(BigDecimal principal, BigDecimal periodRate, int n) {
        if (periodRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        }
        // payment = P * r / (1 - (1+r)^-n)
        BigDecimal onePlusR = BigDecimal.ONE.add(periodRate);
        double onePlusRPowNegN = Math.pow(onePlusR.doubleValue(), -n);
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.valueOf(onePlusRPowNegN));
        return principal.multiply(periodRate).divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal periodRate(BigDecimal annualRatePercent, LoanFrequency frequency) {
        int periodsPerYear = switch (frequency) {
            case WEEKLY -> 52;
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case SEMI_ANNUALLY -> 2;
            case ANNUALLY -> 1;
        };
        return annualRatePercent
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(periodsPerYear), 10, RoundingMode.HALF_UP);
    }

    private LocalDate advance(LocalDate date, LoanFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case SEMI_ANNUALLY -> date.plusMonths(6);
            case ANNUALLY -> date.plusYears(1);
        };
    }

    @Transactional
    public LoanResponse approve(Long id) {
        Loan loan = loadLoan(id);
        if (loan.getStatus() != LoanStatus.DRAFT) {
            throw new BusinessException("Only a draft loan can be approved");
        }
        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());
        return LoanMapper.toResponse(repository.save(loan));
    }

    @Transactional
    public LoanResponse disburse(Long id) {
        Loan loan = loadLoan(id);
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessException("Only an approved loan can be disbursed");
        }

        journalService.postDisbursementJournal(loan);

        BigDecimal principal = loan.getPrincipalAmount();
        boolean capitalizeFees = loan.getTotalFees().compareTo(BigDecimal.ZERO) > 0
                && loan.getFeeTreatment() != null
                && loan.getFeeTreatment() != com.unionsg.xaccounting.enums.loan.LoanFeeTreatment.EXPENSED_IMMEDIATELY;

        loan.setOutstandingPrincipal(capitalizeFees ? principal.add(loan.getTotalFees()) : principal);
        loan.setOutstandingInterest(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursedAt(LocalDateTime.now());

        return LoanMapper.toResponse(repository.save(loan));
    }

    @Transactional
    public LoanResponse recordRepayment(Long id, RecordLoanRepaymentRequest request) {
        Loan loan = loadLoan(id);
        assertRepayable(loan);

        BigDecimal principal = request.getPrincipalAmount() != null ? request.getPrincipalAmount() : BigDecimal.ZERO;
        BigDecimal interest = request.getInterestAmount() != null ? request.getInterestAmount() : BigDecimal.ZERO;
        BigDecimal fees = request.getFeesAmount() != null ? request.getFeesAmount() : BigDecimal.ZERO;
        BigDecimal total = principal.add(interest).add(fees);

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("A repayment must have a positive total amount");
        }
        if (principal.compareTo(loan.getOutstandingPrincipal()) > 0) {
            throw new BusinessException("Principal repayment exceeds outstanding principal");
        }

        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setRepaymentDate(request.getRepaymentDate() != null ? request.getRepaymentDate() : LocalDate.now());
        repayment.setPrincipalAmount(principal);
        repayment.setInterestAmount(interest);
        repayment.setFeesAmount(fees);
        repayment.setTotalAmount(total);
        repayment.setPaymentMethod(request.getPaymentMethod());
        repayment.setReferenceNumber(request.getReferenceNumber());
        repayment.setMemo(request.getMemo());
        if (request.getBankAccountId() != null) {
            repayment.setBankAccount(bankAccountRepository.findById(request.getBankAccountId())
                    .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + request.getBankAccountId())));
        }

        repaymentRepository.save(repayment);
        var journal = journalService.postRepaymentJournal(loan, repayment);
        repayment.setJournal(journal);
        repaymentRepository.save(repayment);

        applyToSchedule(loan, principal, interest);

        loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().subtract(principal));
        loan.setStatus(loan.getOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0
                ? LoanStatus.FULLY_SETTLED : LoanStatus.PARTIALLY_REPAID);
        if (loan.getStatus() == LoanStatus.FULLY_SETTLED) {
            loan.setClosedAt(LocalDateTime.now());
        }

        return LoanMapper.toResponse(repository.save(loan));
    }

    /** Applies a repayment's principal/interest against the oldest not-yet-fully-paid installment(s). */
    private void applyToSchedule(Loan loan, BigDecimal principal, BigDecimal interest) {
        BigDecimal remainingPrincipal = principal;
        BigDecimal remainingInterest = interest;

        for (LoanAmortizationLine line : loan.getSchedule()) {
            if (line.getStatus() == LoanInstallmentStatus.PAID) {
                continue;
            }
            if (remainingPrincipal.compareTo(BigDecimal.ZERO) <= 0 && remainingInterest.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal principalOwed = line.getPrincipalDue().subtract(line.getPrincipalPaid());
            BigDecimal interestOwed = line.getInterestDue().subtract(line.getInterestPaid());

            BigDecimal principalApplied = remainingPrincipal.min(principalOwed).max(BigDecimal.ZERO);
            BigDecimal interestApplied = remainingInterest.min(interestOwed).max(BigDecimal.ZERO);

            line.setPrincipalPaid(line.getPrincipalPaid().add(principalApplied));
            line.setInterestPaid(line.getInterestPaid().add(interestApplied));
            remainingPrincipal = remainingPrincipal.subtract(principalApplied);
            remainingInterest = remainingInterest.subtract(interestApplied);

            boolean fullyPaid = line.getPrincipalPaid().compareTo(line.getPrincipalDue()) >= 0
                    && line.getInterestPaid().compareTo(line.getInterestDue()) >= 0;
            line.setStatus(fullyPaid ? LoanInstallmentStatus.PAID : LoanInstallmentStatus.PARTIALLY_PAID);
        }
    }

    private void assertRepayable(Loan loan) {
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.PARTIALLY_REPAID) {
            throw new BusinessException("Only an active or partially repaid loan can receive a repayment");
        }
    }

    /** Convenience action: pays off the entire remaining outstanding principal/interest in one repayment. */
    @Transactional
    public LoanResponse settle(Long id, RecordLoanRepaymentRequest request) {
        Loan loan = loadLoan(id);
        assertRepayable(loan);

        request.setPrincipalAmount(loan.getOutstandingPrincipal());
        request.setInterestAmount(loan.getOutstandingInterest());
        return recordRepayment(id, request);
    }

    @Transactional
    public LoanResponse accrueInterest(Long id, BigDecimal amount) {
        Loan loan = loadLoan(id);
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.PARTIALLY_REPAID) {
            throw new BusinessException("Only an active or partially repaid loan can accrue interest");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Accrual amount must be positive");
        }

        journalService.postAccrualJournal(loan, amount, LocalDate.now());
        loan.setOutstandingInterest(loan.getOutstandingInterest().add(amount));

        return LoanMapper.toResponse(repository.save(loan));
    }

    @Transactional
    public LoanResponse writeOff(Long id) {
        Loan loan = loadLoan(id);
        if (loan.getDirection() != LoanDirection.LENT) {
            throw new BusinessException("Only a loan the organization lent out can be written off here");
        }
        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.PARTIALLY_REPAID) {
            throw new BusinessException("Only an active or partially repaid loan can be written off");
        }

        BigDecimal remaining = loan.getOutstandingPrincipal();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("This loan has no remaining principal to write off");
        }

        journalService.postWriteOffJournal(loan, remaining, LocalDate.now());
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.WRITTEN_OFF);
        loan.setClosedAt(LocalDateTime.now());

        return LoanMapper.toResponse(repository.save(loan));
    }

    @Transactional
    public LoanResponse cancel(Long id) {
        Loan loan = loadLoan(id);
        if (loan.getStatus() != LoanStatus.DRAFT && loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessException("Only a draft or approved (not yet disbursed) loan can be cancelled");
        }
        loan.setStatus(LoanStatus.CANCELLED);
        loan.setCancelledAt(LocalDateTime.now());
        return LoanMapper.toResponse(repository.save(loan));
    }

    @Transactional(readOnly = true)
    public LoanResponse getById(Long id) {
        return LoanMapper.toResponse(loadLoan(id));
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentResponse> getRepayments(Long id) {
        return LoanMapper.toRepaymentResponses(repaymentRepository.findByLoanIdOrderByRepaymentDateDesc(id));
    }

    @Transactional(readOnly = true)
    public Page<LoanListItemResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(LoanMapper::toListItemResponse);
    }

    private Loan loadLoan(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Loan not found with ID: " + id));
    }

    private AccountEntity resolveAccount(Long accountPk) {
        return accountRepository.findById(accountPk)
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountPk));
    }
}
