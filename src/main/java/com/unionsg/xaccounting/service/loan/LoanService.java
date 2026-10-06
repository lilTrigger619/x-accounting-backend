package com.unionsg.xaccounting.service.loan;

import com.unionsg.xaccounting.MapperLayer.LoanMapper;
import com.unionsg.xaccounting.dto.loan.CreateLoanRequest;
import com.unionsg.xaccounting.dto.loan.LoanAccrualResponse;
import com.unionsg.xaccounting.dto.loan.LoanActionRequest;
import com.unionsg.xaccounting.dto.loan.LoanAuditLogResponse;
import com.unionsg.xaccounting.dto.loan.LoanLineResponse;
import com.unionsg.xaccounting.dto.loan.LoanListItemResponse;
import com.unionsg.xaccounting.dto.loan.LoanRepaymentResponse;
import com.unionsg.xaccounting.dto.loan.LoanResponse;
import com.unionsg.xaccounting.dto.loan.LoanSchedulePreviewResponse;
import com.unionsg.xaccounting.dto.loan.RecordLoanRepaymentRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.loan.Loan;
import com.unionsg.xaccounting.entity.loan.LoanAmortizationLine;
import com.unionsg.xaccounting.entity.loan.LoanInterestAccrual;
import com.unionsg.xaccounting.entity.loan.LoanRepayment;
import com.unionsg.xaccounting.entity.loan.LoanType;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.loan.LoanAuditAction;
import com.unionsg.xaccounting.enums.loan.LoanCounterpartyType;
import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanFeeTreatment;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.enums.loan.LoanPaymentStatus;
import com.unionsg.xaccounting.enums.loan.LoanPaymentType;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.loan.LoanAuditLogRepository;
import com.unionsg.xaccounting.repository.loan.LoanInterestAccrualRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepaymentRepository;
import com.unionsg.xaccounting.repository.loan.LoanRepository;
import com.unionsg.xaccounting.repository.loan.LoanTypeRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.security.util.SecurityUtils;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Loan lifecycle: create and edit a draft with its generated schedule, approve, disburse (GL
 * posting), take payments (scheduled, early, partial, overpayment), mark missed installments,
 * accrue interest, default, close (with a write-off for a lent loan), cancel, and reverse
 * payments or the whole loan through reversal journals.
 *
 * <p>A payment's split into fees, interest and principal is fixed when it is recorded. The
 * schedule's paid amounts are always the result of applying the standing payments, in the
 * order they were recorded, to the schedule as generated, so reversing any payment is a clean
 * rebuild rather than an attempt to un-apply it.</p>
 */
@Service
@RequiredArgsConstructor
public class LoanService {

    static final String CURRENCY_CONFIG = "currencies";

    /** Statuses in which the schedule is running and payments are accepted. */
    static final Set<LoanStatus> LIVE = EnumSet.of(LoanStatus.ACTIVE, LoanStatus.PARTIALLY_PAID, LoanStatus.DEFAULTED);

    private final LoanRepository repository;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final LoanInterestAccrualRepository accrualRepository;
    private final LoanAuditLogRepository auditLogRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final DocumentNumberService documentNumberService;
    private final ConfigValueValidator configValueValidator;
    private final LoanJournalService journalService;
    private final LoanAuditService auditService;

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    @Transactional
    public LoanResponse create(CreateLoanRequest request) {
        Loan loan = new Loan();
        applyRequest(loan, request);
        loan.setStatus(LoanStatus.DRAFT);
        loan.setLoanNumber(documentNumberService.generateNextNumber(DocumentModule.LOAN));
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.CREATED, null,
                saved.getDirection().getLabel() + " of " + saved.getPrincipalAmount() + " " + saved.getCurrency());
        return LoanMapper.toResponse(saved);
    }

    @Transactional
    public LoanResponse update(Long id, CreateLoanRequest request) {
        Loan loan = loadLoan(id);
        if (loan.getStatus() != LoanStatus.DRAFT) {
            throw new BusinessException("Only a draft loan can be edited. This loan is " + label(loan.getStatus()) + ".");
        }
        applyRequest(loan, request);
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.UPDATED, LoanStatus.DRAFT, "Loan terms edited; schedule regenerated");
        return LoanMapper.toResponse(saved);
    }

    /** Soft-deletes a loan that never reached the ledger (draft or cancelled before disbursement). */
    @Transactional
    public void delete(Long id) {
        Loan loan = loadLoan(id);
        if (loan.getStatus() != LoanStatus.DRAFT && loan.getStatus() != LoanStatus.CANCELLED) {
            throw new BusinessException("Only a draft or cancelled loan can be deleted. A loan that has been disbursed "
                    + "keeps its history; reverse it instead.");
        }
        if (loan.getJournal() != null) {
            throw new BusinessException("This loan has posted journals and cannot be deleted");
        }
        LoanStatus previous = loan.getStatus();
        var user = SecurityUtils.getCurrentUser();
        loan.softDelete(user != null ? String.valueOf(user.getId()) : null);
        repository.save(loan);
        auditService.record(loan, LoanAuditAction.DELETED, previous, null);
    }

    /** Works out the schedule for unsaved terms, for the New Loan form. */
    @Transactional(readOnly = true)
    public LoanSchedulePreviewResponse preview(CreateLoanRequest request) {
        Loan loan = new Loan();
        copyTerms(loan, request);
        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(terms(loan, request));
        LoanSchedulePreviewResponse r = new LoanSchedulePreviewResponse();
        r.setLines(lines.stream().map(l -> LoanMapper.toLineResponse(l, LocalDate.now(), false)).toList());
        r.setMaturityDate(lines.get(lines.size() - 1).getDueDate());
        r.setFinancedAmount(financedAmount(loan));
        r.setTotalPrincipal(sum(lines, LoanAmortizationLine::getPrincipalDue));
        r.setTotalInterest(sum(lines, LoanAmortizationLine::getInterestDue));
        r.setTotalFees(sum(lines, LoanAmortizationLine::getFeesDue));
        r.setTotalPayable(sum(lines, LoanAmortizationLine::getTotalInstallment));
        return r;
    }

    private void applyRequest(Loan loan, CreateLoanRequest request) {
        if (request.getDirection() == null) {
            throw new BusinessException("Choose whether this is a borrowed or a lent loan");
        }
        if (request.getLoanTypeId() == null) {
            throw new BusinessException("Loan type is required");
        }
        LoanType loanType = loanTypeRepository.findById(request.getLoanTypeId())
                .filter(t -> !Boolean.TRUE.equals(t.getDeleted()))
                .orElseThrow(() -> new BusinessException("Loan type not found with ID: " + request.getLoanTypeId()));
        boolean unchangedType = loan.getLoanType() != null && loan.getLoanType().getId().equals(loanType.getId());
        if (!unchangedType && !Boolean.TRUE.equals(loanType.getActive())) {
            throw new BusinessException("Loan type \"" + loanType.getName() + "\" is inactive and cannot be used");
        }
        loan.setLoanType(loanType);
        loan.setDirection(request.getDirection());
        if (request.getCounterpartyType() == null) {
            throw new BusinessException("Counterparty type is required");
        }
        loan.setCounterpartyType(request.getCounterpartyType());
        resolveCounterparty(loan, request);

        String previousCurrency = loan.getCurrency();
        String currency = configValueValidator.validate(CURRENCY_CONFIG, request.getCurrency(), previousCurrency, "Currency");
        if (currency == null) {
            currency = configValueValidator.defaultValue(CURRENCY_CONFIG);
        }
        if (currency == null) {
            throw new BusinessException("Currency is required. Pick one, or set a default currency under Settings > Configurations.");
        }
        loan.setCurrency(currency);

        copyTerms(loan, request);
        if (request.getStartDate() == null) {
            throw new BusinessException("Start date is required");
        }

        loan.setBankAccount(request.getBankAccountId() == null ? null
                : bankAccountRepository.findById(request.getBankAccountId())
                .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + request.getBankAccountId())));
        loan.setPrincipalAccount(request.getPrincipalAccountId() == null ? null : resolveAccount(request.getPrincipalAccountId()));
        loan.setInterestAccount(request.getInterestAccountId() == null ? null : resolveAccount(request.getInterestAccountId()));
        if (loan.getPrincipalAccount() != null && loan.getInterestAccount() != null
                && loan.getPrincipalAccount().getId().equals(loan.getInterestAccount().getId())) {
            throw new BusinessException("The principal account and the interest account must be different accounts");
        }

        loan.setCollateralDescription(trimToNull(request.getCollateralDescription()));
        if (request.getCollateralValue() != null && request.getCollateralValue().signum() < 0) {
            throw new BusinessException("Collateral value cannot be negative");
        }
        loan.setCollateralValue(request.getCollateralValue());
        loan.setExternalReference(trimToNull(request.getExternalReference()));
        loan.setNotes(trimToNull(request.getNotes()));
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setOutstandingInterest(BigDecimal.ZERO);

        List<LoanAmortizationLine> lines = LoanScheduleEngine.generate(terms(loan, request));
        loan.getSchedule().clear();
        for (LoanAmortizationLine line : lines) {
            line.setLoan(loan);
            loan.getSchedule().add(line);
        }
        loan.setNumberOfInstallments(lines.size());
        loan.setMaturityDate(lines.get(lines.size() - 1).getDueDate());
    }

    /** Copies the numeric terms that drive the schedule onto the loan, with validation. */
    private void copyTerms(Loan loan, CreateLoanRequest request) {
        if (request.getPrincipalAmount() == null || request.getPrincipalAmount().signum() <= 0) {
            throw new BusinessException("Principal amount must be greater than zero");
        }
        loan.setPrincipalAmount(LoanScheduleEngine.money(request.getPrincipalAmount()));
        BigDecimal rate = request.getInterestRate() != null ? request.getInterestRate() : BigDecimal.ZERO;
        if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(1000)) > 0) {
            throw new BusinessException("Interest rate must be between 0 and 1000 percent a year");
        }
        loan.setInterestRate(rate);
        if (request.getInterestMethod() == null) {
            throw new BusinessException("Interest calculation method is required");
        }
        loan.setInterestMethod(request.getInterestMethod());
        loan.setStartDate(request.getStartDate());
        if (request.getPaymentFrequency() == null) {
            throw new BusinessException("Payment frequency is required");
        }
        loan.setPaymentFrequency(request.getPaymentFrequency());
        if (request.getInterestMethod() == LoanInterestMethod.CUSTOM_SCHEDULE) {
            int count = request.getCustomSchedule() != null ? request.getCustomSchedule().size() : 0;
            loan.setNumberOfInstallments(count);
            loan.setGracePeriodInstallments(0);
        } else {
            if (request.getNumberOfInstallments() == null || request.getNumberOfInstallments() <= 0) {
                throw new BusinessException("Number of installments must be at least 1");
            }
            if (request.getNumberOfInstallments() > 1200) {
                throw new BusinessException("Number of installments cannot exceed 1200");
            }
            loan.setNumberOfInstallments(request.getNumberOfInstallments());
            loan.setGracePeriodInstallments(request.getGracePeriodInstallments() != null ? request.getGracePeriodInstallments() : 0);
        }
        BigDecimal fees = request.getTotalFees() != null ? request.getTotalFees() : BigDecimal.ZERO;
        BigDecimal installmentFee = request.getInstallmentFee() != null ? request.getInstallmentFee() : BigDecimal.ZERO;
        if (fees.signum() < 0 || installmentFee.signum() < 0) {
            throw new BusinessException("Fees cannot be negative");
        }
        loan.setTotalFees(LoanScheduleEngine.money(fees));
        loan.setInstallmentFee(LoanScheduleEngine.money(installmentFee));
        loan.setFeeTreatment(fees.signum() > 0
                ? (request.getFeeTreatment() != null ? request.getFeeTreatment() : LoanFeeTreatment.EXPENSED_IMMEDIATELY)
                : request.getFeeTreatment());
        if (loan.getFeeTreatment() == LoanFeeTreatment.EXPENSED_IMMEDIATELY && fees.compareTo(loan.getPrincipalAmount()) >= 0) {
            throw new BusinessException("Fees deducted at disbursement must be less than the principal");
        }
        loan.setAllowOverpayment(Boolean.TRUE.equals(request.getAllowOverpayment()));
    }

    private static LoanScheduleEngine.Terms terms(Loan loan, CreateLoanRequest request) {
        return new LoanScheduleEngine.Terms(
                financedAmount(loan), loan.getInterestRate(), loan.getPaymentFrequency(),
                loan.getNumberOfInstallments(), loan.getGracePeriodInstallments(), loan.getInterestMethod(),
                loan.getStartDate(), loan.getInstallmentFee(),
                request != null ? request.getCustomSchedule() : null);
    }

    /** Principal plus fees added to the loan balance. The schedule repays this amount. */
    static BigDecimal financedAmount(Loan loan) {
        BigDecimal fees = loan.getTotalFees() != null ? loan.getTotalFees() : BigDecimal.ZERO;
        return loan.getFeeTreatment() == LoanFeeTreatment.CAPITALIZED
                ? loan.getPrincipalAmount().add(fees)
                : loan.getPrincipalAmount();
    }

    private void resolveCounterparty(Loan loan, CreateLoanRequest request) {
        LoanCounterpartyType type = request.getCounterpartyType();
        loan.setEmployee(null);
        loan.setCustomer(null);
        loan.setSupplier(null);
        if (type == LoanCounterpartyType.EMPLOYEE) {
            if (request.getEmployeeId() == null) throw new BusinessException("Select the employee");
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new BusinessException("Employee not found with ID: " + request.getEmployeeId()));
            loan.setEmployee(employee);
            loan.setCounterpartyName(employee.getFullName());
        } else if (type == LoanCounterpartyType.CUSTOMER) {
            if (request.getCustomerId() == null) throw new BusinessException("Select the customer");
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new BusinessException("Customer not found with ID: " + request.getCustomerId()));
            loan.setCustomer(customer);
            loan.setCounterpartyName(customer.getDisplayName());
        } else if (type == LoanCounterpartyType.SUPPLIER) {
            if (request.getSupplierId() == null) throw new BusinessException("Select the supplier");
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new BusinessException("Supplier not found with ID: " + request.getSupplierId()));
            loan.setSupplier(supplier);
            loan.setCounterpartyName(supplier.getDisplayName());
        } else {
            if (request.getCounterpartyName() == null || request.getCounterpartyName().isBlank()) {
                throw new BusinessException("A counterparty name is required for this counterparty type");
            }
            loan.setCounterpartyName(request.getCounterpartyName().trim());
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    @Transactional
    public LoanResponse approve(Long id) {
        Loan loan = loadLoan(id);
        requireStatus(loan, "approved", LoanStatus.DRAFT);
        loan.setStatus(LoanStatus.APPROVED);
        loan.setApprovedAt(LocalDateTime.now());
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.APPROVED, LoanStatus.DRAFT, null);
        return LoanMapper.toResponse(saved);
    }

    /** Posts the disbursement journal (dated the start date unless given) and starts the schedule. */
    @Transactional
    public LoanResponse disburse(Long id, LoanActionRequest request) {
        Loan loan = loadLoan(id);
        requireStatus(loan, "disbursed", LoanStatus.APPROVED);
        LocalDate date = request != null && request.getDate() != null ? request.getDate() : loan.getStartDate();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("A loan cannot be disbursed with a future date (" + date + ")");
        }

        loan.setJournal(journalService.postDisbursementJournal(loan, date));
        loan.setOutstandingPrincipal(financedAmount(loan));
        loan.setOutstandingInterest(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setDisbursedAt(LocalDateTime.now());

        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.DISBURSED, LoanStatus.APPROVED,
                "Journal " + saved.getJournal().getJournalNumber() + " dated " + date);
        return LoanMapper.toResponse(saved);
    }

    @Transactional
    public LoanResponse recordRepayment(Long id, RecordLoanRepaymentRequest request) {
        Loan loan = loadLoan(id);
        if (!LIVE.contains(loan.getStatus())) {
            throw new BusinessException("Payments can only be recorded on an active, partially paid or defaulted loan. "
                    + "This loan is " + label(loan.getStatus()) + ".");
        }
        LocalDate date = request.getRepaymentDate() != null ? request.getRepaymentDate() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException("A payment cannot be dated in the future");
        }
        if (date.isBefore(loan.getStartDate())) {
            throw new BusinessException("A payment cannot be dated before the loan started (" + loan.getStartDate() + ")");
        }
        if (request.getPaymentMethod() == null) {
            throw new BusinessException("Payment method is required");
        }

        List<LoanAmortizationLine> lines = loan.getSchedule();
        BigDecimal dueNow = LoanScheduleEngine.dueAsOf(lines, date);
        LoanScheduleEngine.Split split = request.hasManualSplit()
                ? manualSplit(loan, request)
                : autoSplit(lines, date, request.getAmount());

        if (split.overpayment().signum() > 0 && !loan.isOverpaymentAllowed()) {
            BigDecimal max = split.total().subtract(split.overpayment());
            throw new BusinessException("This payment is " + split.overpayment() + " more than the loan's outstanding balance. "
                    + "The most that can be paid now is " + max + ". Turn on \"Allow overpayment\" on the loan to accept more.");
        }

        LoanRepayment p = new LoanRepayment();
        p.setLoan(loan);
        p.setRepaymentDate(date);
        p.setFeesAmount(split.fees());
        p.setInterestAmount(split.interest());
        p.setPrincipalAmount(split.principal());
        p.setOverpaymentAmount(split.overpayment());
        p.setTotalAmount(split.total());
        p.setAccruedInterestApplied(split.interest().min(nz(loan.getOutstandingInterest())));
        p.setPaymentType(classify(split, dueNow, LoanScheduleEngine.totalOwed(lines)));
        p.setStatus(LoanPaymentStatus.POSTED);
        p.setPaymentMethod(request.getPaymentMethod());
        p.setReferenceNumber(trimToNull(request.getReferenceNumber()));
        p.setMemo(trimToNull(request.getMemo()));
        if (request.getBankAccountId() != null) {
            p.setBankAccount(bankAccountRepository.findById(request.getBankAccountId())
                    .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + request.getBankAccountId())));
        } else {
            p.setBankAccount(loan.getBankAccount());
        }
        repaymentRepository.save(p);
        p.setJournal(journalService.postRepaymentJournal(loan, p));
        repaymentRepository.save(p);

        LoanStatus previous = loan.getStatus();
        LoanScheduleEngine.apply(lines, new LoanScheduleEngine.Split(split.fees(), split.interest(), split.principal(), BigDecimal.ZERO),
                date, terms(loan, null));
        loan.setOutstandingPrincipal(sumOwedPrincipal(lines));
        loan.setOutstandingInterest(nz(loan.getOutstandingInterest()).subtract(p.getAccruedInterestApplied()));
        loan.setOverpaymentBalance(loan.getOverpaymentBalance().add(split.overpayment()));
        refreshStatus(loan, true);

        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.PAYMENT_RECORDED, previous,
                label(p.getPaymentType()) + " payment of " + p.getTotalAmount() + " (principal " + p.getPrincipalAmount()
                        + ", interest " + p.getInterestAmount() + ", fees " + p.getFeesAmount()
                        + (p.getOverpaymentAmount().signum() > 0 ? ", overpayment " + p.getOverpaymentAmount() : "")
                        + "), journal " + p.getJournal().getJournalNumber());
        return LoanMapper.toResponse(saved);
    }

    private LoanScheduleEngine.Split autoSplit(List<LoanAmortizationLine> lines, LocalDate date, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        return LoanScheduleEngine.autoSplit(lines, date, LoanScheduleEngine.money(amount));
    }

    /** A split the user entered: fees and interest cannot exceed what the schedule still holds. */
    private LoanScheduleEngine.Split manualSplit(Loan loan, RecordLoanRepaymentRequest request) {
        BigDecimal fees = LoanScheduleEngine.money(nz(request.getFeesAmount()));
        BigDecimal interest = LoanScheduleEngine.money(nz(request.getInterestAmount()));
        BigDecimal principal = LoanScheduleEngine.money(nz(request.getPrincipalAmount()));
        if (fees.signum() < 0 || interest.signum() < 0 || principal.signum() < 0) {
            throw new BusinessException("Payment amounts cannot be negative");
        }
        if (fees.add(interest).add(principal).signum() <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }
        List<LoanAmortizationLine> lines = loan.getSchedule();
        BigDecimal feesOwed = sum(lines, LoanAmortizationLine::feesOwed);
        BigDecimal interestOwed = sum(lines, LoanAmortizationLine::interestOwed);
        if (fees.compareTo(feesOwed) > 0) {
            throw new BusinessException("Fees paid (" + fees + ") exceed the fees still owed on the schedule (" + feesOwed + ")");
        }
        if (interest.compareTo(interestOwed) > 0) {
            throw new BusinessException("Interest paid (" + interest + ") exceeds the interest still owed on the schedule (" + interestOwed + ")");
        }
        BigDecimal outstanding = sumOwedPrincipal(lines);
        BigDecimal over = principal.subtract(outstanding).max(BigDecimal.ZERO);
        return new LoanScheduleEngine.Split(fees, interest, principal.subtract(over), over);
    }

    static LoanPaymentType classify(LoanScheduleEngine.Split split, BigDecimal dueNow, BigDecimal totalOwed) {
        BigDecimal paid = split.total();
        if (split.overpayment().signum() > 0) return LoanPaymentType.OVERPAYMENT;
        if (dueNow.signum() == 0) return LoanPaymentType.EARLY;
        int c = paid.compareTo(dueNow);
        if (c < 0) return LoanPaymentType.PARTIAL;
        if (c == 0) return LoanPaymentType.SCHEDULED;
        return LoanPaymentType.EARLY;
    }

    @Transactional
    public LoanResponse reversePayment(Long id, Long paymentId, LoanActionRequest request) {
        Loan loan = loadLoan(id);
        if (!LIVE.contains(loan.getStatus()) && loan.getStatus() != LoanStatus.FULLY_PAID) {
            throw new BusinessException("Payments can only be reversed while the loan is open. This loan is "
                    + label(loan.getStatus()) + ".");
        }
        String reason = requireReason(request);
        LoanRepayment p = repaymentRepository.findById(paymentId)
                .filter(r -> r.getLoan().getId().equals(loan.getId()))
                .orElseThrow(() -> new BusinessException("Payment not found on this loan"));
        if (!p.isPosted()) {
            throw new BusinessException("This payment has already been reversed");
        }

        if (p.getJournal() != null) {
            p.setReversalJournal(journalService.reverse(p.getJournal(), "Reversal of loan payment: " + reason));
        }
        p.setStatus(LoanPaymentStatus.REVERSED);
        p.setReversedAt(LocalDateTime.now());
        p.setReversalReason(reason);
        repaymentRepository.save(p);

        LoanStatus previous = loan.getStatus();
        rebuild(loan);
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.PAYMENT_REVERSED, previous,
                "Payment of " + p.getTotalAmount() + " on " + p.getRepaymentDate() + " reversed: " + reason);
        return LoanMapper.toResponse(saved);
    }

    @Transactional
    public LoanResponse markInstallmentMissed(Long id, Integer installmentNumber, LoanActionRequest request) {
        Loan loan = loadLoan(id);
        if (!LIVE.contains(loan.getStatus())) {
            throw new BusinessException("Only an installment of an active, partially paid or defaulted loan can be marked as missed");
        }
        LoanAmortizationLine line = loan.getSchedule().stream()
                .filter(l -> l.getInstallmentNumber().equals(installmentNumber))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Installment " + installmentNumber + " not found"));
        if (line.getDueDate().isAfter(LocalDate.now())) {
            throw new BusinessException("Installment " + installmentNumber + " is not due until " + line.getDueDate());
        }
        if (line.totalOwed().signum() == 0) {
            throw new BusinessException("Installment " + installmentNumber + " is already paid");
        }
        line.setMissed(true);
        line.setMissedNote(request != null ? trimToNull(request.getReason()) : null);
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.INSTALLMENT_MISSED, loan.getStatus(),
                "Installment " + installmentNumber + " due " + line.getDueDate() + " missed; " + line.totalOwed() + " unpaid"
                        + (line.getMissedNote() != null ? ": " + line.getMissedNote() : ""));
        return LoanMapper.toResponse(saved);
    }

    @Transactional
    public LoanResponse accrueInterest(Long id, LoanActionRequest request) {
        Loan loan = loadLoan(id);
        if (!LIVE.contains(loan.getStatus())) {
            throw new BusinessException("Interest can only be accrued on an active, partially paid or defaulted loan");
        }
        BigDecimal amount = request != null && request.getAmount() != null ? LoanScheduleEngine.money(request.getAmount()) : null;
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException("Accrual amount must be greater than zero");
        }
        BigDecimal accruable = sum(loan.getSchedule(), LoanAmortizationLine::interestOwed).subtract(nz(loan.getOutstandingInterest()));
        if (amount.compareTo(accruable) > 0) {
            throw new BusinessException("Only " + accruable.max(BigDecimal.ZERO) + " of scheduled interest is left to accrue");
        }
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        if (date.isBefore(loan.getStartDate()) || date.isAfter(LocalDate.now())) {
            throw new BusinessException("Accrual date must be between the loan start date and today");
        }

        LoanInterestAccrual accrual = new LoanInterestAccrual();
        accrual.setLoan(loan);
        accrual.setAccrualDate(date);
        accrual.setAmount(amount);
        accrual.setMemo(trimToNull(request.getReason()));
        accrualRepository.save(accrual);
        accrual.setJournal(journalService.postAccrualJournal(loan, amount, date, accrual.getId()));
        accrualRepository.save(accrual);

        loan.setOutstandingInterest(nz(loan.getOutstandingInterest()).add(amount));
        loan.setAccruedInterestTotal(loan.getAccruedInterestTotal().add(amount));
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.INTEREST_ACCRUED, saved.getStatus(),
                "Accrued " + amount + " dated " + date + ", journal " + accrual.getJournal().getJournalNumber());
        return LoanMapper.toResponse(saved);
    }

    @Transactional
    public LoanResponse markDefaulted(Long id, LoanActionRequest request) {
        Loan loan = loadLoan(id);
        requireStatus(loan, "marked as defaulted", LoanStatus.ACTIVE, LoanStatus.PARTIALLY_PAID);
        String reason = requireReason(request);
        LoanStatus previous = loan.getStatus();
        loan.setStatus(LoanStatus.DEFAULTED);
        loan.setStatusReason(reason);
        loan.setDefaultedAt(LocalDateTime.now());
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.DEFAULTED, previous, reason);
        return LoanMapper.toResponse(saved);
    }

    /**
     * Closes a loan. A fully paid loan just closes. A lent loan that still has a balance closes
     * only with {@code writeOff}: the unpaid principal and accrued interest are written off.
     */
    @Transactional
    public LoanResponse close(Long id, LoanActionRequest request) {
        Loan loan = loadLoan(id);
        LoanStatus previous = loan.getStatus();
        if (loan.getOverpaymentBalance().signum() > 0) {
            throw new BusinessException("This loan holds an overpayment of " + loan.getOverpaymentBalance()
                    + ". Reverse the payment that overpaid and record the correct amount before closing it.");
        }
        String reason = request != null ? trimToNull(request.getReason()) : null;

        if (loan.getStatus() == LoanStatus.FULLY_PAID) {
            loan.setStatus(LoanStatus.CLOSED);
        } else if (LIVE.contains(loan.getStatus())) {
            boolean writeOff = request != null && Boolean.TRUE.equals(request.getWriteOff());
            if (loan.getDirection() == LoanDirection.BORROWED_LOAN) {
                throw new BusinessException("A borrowed loan can only be closed once it is fully paid. "
                        + sum(loan.getSchedule(), LoanAmortizationLine::totalOwed) + " is still owed.");
            }
            if (!writeOff) {
                throw new BusinessException("This loan still has " + sum(loan.getSchedule(), LoanAmortizationLine::totalOwed)
                        + " outstanding. Close it with a write-off, or record the remaining payments first.");
            }
            if (reason == null) {
                throw new BusinessException("Give a reason for writing off the loan");
            }
            LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
            BigDecimal principal = loan.getOutstandingPrincipal();
            BigDecimal accrued = nz(loan.getOutstandingInterest());
            if (principal.add(accrued).signum() > 0) {
                loan.setWriteOffJournal(journalService.postWriteOffJournal(loan, principal, accrued, date));
            }
            loan.setWrittenOffAmount(principal.add(accrued));
            loan.setOutstandingPrincipal(BigDecimal.ZERO);
            loan.setOutstandingInterest(BigDecimal.ZERO);
            loan.setStatus(LoanStatus.CLOSED);
            auditService.record(loan, LoanAuditAction.WRITTEN_OFF, previous,
                    "Wrote off principal " + principal + " and accrued interest " + accrued + ": " + reason);
        } else {
            throw new BusinessException("Only a fully paid loan, or a lent loan being written off, can be closed. "
                    + "This loan is " + label(loan.getStatus()) + ".");
        }
        loan.setStatusReason(reason);
        loan.setClosedAt(LocalDateTime.now());
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.CLOSED, previous, reason);
        return LoanMapper.toResponse(saved);
    }

    @Transactional
    public LoanResponse cancel(Long id, LoanActionRequest request) {
        Loan loan = loadLoan(id);
        requireStatus(loan, "cancelled", LoanStatus.DRAFT, LoanStatus.APPROVED);
        LoanStatus previous = loan.getStatus();
        String reason = request != null ? trimToNull(request.getReason()) : null;
        loan.setStatus(LoanStatus.CANCELLED);
        loan.setStatusReason(reason);
        loan.setCancelledAt(LocalDateTime.now());
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.CANCELLED, previous, reason);
        return LoanMapper.toResponse(saved);
    }

    /**
     * Undoes a disbursed loan: every standing payment, accrual and the disbursement are reversed
     * with reversal journals (newest first). The original journals stay in the ledger, marked reversed.
     */
    @Transactional
    public LoanResponse reverseLoan(Long id, LoanActionRequest request) {
        Loan loan = loadLoan(id);
        requireStatus(loan, "reversed", LoanStatus.ACTIVE, LoanStatus.PARTIALLY_PAID, LoanStatus.FULLY_PAID, LoanStatus.DEFAULTED);
        String reason = requireReason(request);
        LoanStatus previous = loan.getStatus();

        int payments = 0;
        List<LoanRepayment> standing = repaymentRepository.findByLoanIdOrderByRepaymentDateDescIdDesc(loan.getId()).stream()
                .filter(LoanRepayment::isPosted).toList();
        for (LoanRepayment p : standing) {
            if (p.getJournal() != null) {
                p.setReversalJournal(journalService.reverse(p.getJournal(), "Loan " + loan.getLoanNumber() + " reversed: " + reason));
            }
            p.setStatus(LoanPaymentStatus.REVERSED);
            p.setReversedAt(LocalDateTime.now());
            p.setReversalReason("Loan reversed: " + reason);
            repaymentRepository.save(p);
            payments++;
        }
        List<LoanInterestAccrual> accruals = new ArrayList<>(accrualRepository.findByLoanIdOrderByAccrualDateAscIdAsc(loan.getId()));
        accruals.sort(Comparator.comparing(LoanInterestAccrual::getId).reversed());
        for (LoanInterestAccrual a : accruals) {
            if (Boolean.TRUE.equals(a.getReversed())) continue;
            if (a.getJournal() != null) {
                journalService.reverse(a.getJournal(), "Loan " + loan.getLoanNumber() + " reversed: " + reason);
            }
            a.setReversed(true);
            accrualRepository.save(a);
        }
        if (loan.getJournal() != null) {
            journalService.reverse(loan.getJournal(), "Loan " + loan.getLoanNumber() + " reversed: " + reason);
        }

        loan.getSchedule().forEach(l -> {
            l.resetToOriginal();
            l.setMissed(false);
        });
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setOutstandingInterest(BigDecimal.ZERO);
        loan.setOverpaymentBalance(BigDecimal.ZERO);
        loan.setStatus(LoanStatus.REVERSED);
        loan.setStatusReason(reason);
        loan.setReversedAt(LocalDateTime.now());
        Loan saved = repository.save(loan);
        auditService.record(saved, LoanAuditAction.REVERSED, previous,
                "Reversed the disbursement, " + payments + " payment(s) and " + accruals.stream().filter(a -> a.getJournal() != null).count()
                        + " accrual(s): " + reason);
        return LoanMapper.toResponse(saved);
    }

    // ------------------------------------------------------------------
    // Rebuild
    // ------------------------------------------------------------------

    /**
     * Puts the schedule back as generated and re-applies every standing payment in the order it
     * was recorded, then restates balances and status. Principal that no longer fits (because an
     * earlier payment was reversed) is held as an overpayment, exactly as the GL holds it.
     */
    void rebuild(Loan loan) {
        List<LoanAmortizationLine> lines = loan.getSchedule();
        lines.forEach(LoanAmortizationLine::resetToOriginal);
        LoanScheduleEngine.Terms terms = terms(loan, null);

        BigDecimal overpayment = BigDecimal.ZERO;
        BigDecimal accruedApplied = BigDecimal.ZERO;
        List<LoanRepayment> standing = repaymentRepository.findByLoanIdOrderByRepaymentDateAscIdAsc(loan.getId()).stream()
                .filter(LoanRepayment::isPosted)
                .sorted(Comparator.comparing(LoanRepayment::getId))
                .toList();
        for (LoanRepayment p : standing) {
            BigDecimal principal = p.getPrincipalAmount().add(p.getOverpaymentAmount());
            BigDecimal fits = principal.min(sumOwedPrincipal(lines));
            BigDecimal fees = p.getFeesAmount().min(sum(lines, LoanAmortizationLine::feesOwed));
            BigDecimal interest = p.getInterestAmount().min(sum(lines, LoanAmortizationLine::interestOwed));
            LoanScheduleEngine.apply(lines, new LoanScheduleEngine.Split(fees, interest, fits, BigDecimal.ZERO),
                    p.getRepaymentDate(), terms);
            overpayment = overpayment.add(principal.subtract(fits))
                    .add(p.getFeesAmount().subtract(fees))
                    .add(p.getInterestAmount().subtract(interest));
            accruedApplied = accruedApplied.add(p.getAccruedInterestApplied());
        }
        BigDecimal accrued = accrualRepository.findByLoanIdOrderByAccrualDateAscIdAsc(loan.getId()).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getReversed()))
                .map(LoanInterestAccrual::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        loan.setOutstandingPrincipal(sumOwedPrincipal(lines));
        loan.setOutstandingInterest(accrued.subtract(accruedApplied).max(BigDecimal.ZERO));
        loan.setOverpaymentBalance(overpayment);
        refreshStatus(loan, !standing.isEmpty());
    }

    private void refreshStatus(Loan loan, boolean anyPayments) {
        boolean settled = sum(loan.getSchedule(), LoanAmortizationLine::totalOwed).signum() == 0;
        if (settled) {
            loan.setStatus(LoanStatus.FULLY_PAID);
        } else if (loan.getStatus() == LoanStatus.DEFAULTED) {
            // stays defaulted until settled or closed
        } else {
            loan.setStatus(anyPayments ? LoanStatus.PARTIALLY_PAID : LoanStatus.ACTIVE);
        }
    }

    // ------------------------------------------------------------------
    // Enquiry
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public LoanResponse getById(Long id) {
        return LoanMapper.toResponse(loadLoan(id));
    }

    @Transactional(readOnly = true)
    public List<LoanRepaymentResponse> getRepayments(Long id) {
        loadLoan(id);
        return LoanMapper.toRepaymentResponses(repaymentRepository.findByLoanIdOrderByRepaymentDateDescIdDesc(id));
    }

    @Transactional(readOnly = true)
    public List<LoanAccrualResponse> getAccruals(Long id) {
        loadLoan(id);
        return accrualRepository.findByLoanIdOrderByAccrualDateAscIdAsc(id).stream().map(LoanMapper::toAccrualResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanAuditLogResponse> getActivity(Long id) {
        loadLoan(id);
        return auditLogRepository.findByLoanIdOrderByPerformedAtDescIdDesc(id).stream().map(LoanMapper::toAuditResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LoanLineResponse> getSchedule(Long id) {
        return LoanMapper.toResponse(loadLoan(id)).getSchedule();
    }

    @Transactional(readOnly = true)
    public Page<LoanListItemResponse> list(LoanSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(spec(criteria), pageable).map(LoanMapper::toListItemResponse);
    }

    private static Specification<Loan> spec(LoanSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            if (c != null) {
                if (c.search() != null && !c.search().isBlank()) {
                    String like = "%" + c.search().trim().toLowerCase() + "%";
                    p.add(cb.or(
                            cb.like(cb.lower(root.get("loanNumber")), like),
                            cb.like(cb.lower(root.get("counterpartyName")), like),
                            cb.like(cb.lower(cb.coalesce(root.get("externalReference"), "")), like)));
                }
                if (c.direction() != null) p.add(cb.equal(root.get("direction"), c.direction()));
                if (c.status() != null) p.add(cb.equal(root.get("status"), c.status()));
                if (c.loanTypeId() != null) p.add(cb.equal(root.get("loanType").get("id"), c.loanTypeId()));
                if (c.currency() != null && !c.currency().isBlank()) p.add(cb.equal(root.get("currency"), c.currency()));
                if (c.startFrom() != null) p.add(cb.greaterThanOrEqualTo(root.get("startDate"), c.startFrom()));
                if (c.startTo() != null) p.add(cb.lessThanOrEqualTo(root.get("startDate"), c.startTo()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    Loan loadLoan(Long id) {
        return repository.findById(id)
                .filter(l -> !Boolean.TRUE.equals(l.getDeleted()))
                .orElseThrow(() -> new BusinessException("Loan not found with ID: " + id));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static void requireStatus(Loan loan, String action, LoanStatus... allowed) {
        for (LoanStatus s : allowed) {
            if (loan.getStatus() == s) return;
        }
        List<String> names = java.util.Arrays.stream(allowed).map(LoanService::label).toList();
        throw new BusinessException("Only a " + String.join(" or ", names) + " loan can be " + action
                + ". This loan is " + label(loan.getStatus()) + ".");
    }

    private static String requireReason(LoanActionRequest request) {
        String reason = request != null ? trimToNull(request.getReason()) : null;
        if (reason == null) {
            throw new BusinessException("A reason is required");
        }
        return reason;
    }

    static String label(Enum<?> value) {
        return value == null ? "" : value.name().toLowerCase().replace('_', ' ');
    }

    private AccountEntity resolveAccount(Long accountPk) {
        return accountRepository.findById(accountPk)
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountPk));
    }

    private static BigDecimal sumOwedPrincipal(List<LoanAmortizationLine> lines) {
        return sum(lines, LoanAmortizationLine::principalOwed);
    }

    static BigDecimal sum(List<LoanAmortizationLine> lines, java.util.function.Function<LoanAmortizationLine, BigDecimal> f) {
        return lines.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String trimToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** List filters. Any of them may be null. */
    public record LoanSearchCriteria(String search, LoanDirection direction, LoanStatus status, Long loanTypeId,
                                     String currency, LocalDate startFrom, LocalDate startTo) {
    }
}
