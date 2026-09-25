package com.unionsg.xaccounting.service.prepayment;

import com.unionsg.xaccounting.MapperLayer.PrepaymentMapper;
import com.unionsg.xaccounting.dto.prepayment.CreatePrepaymentRequest;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentListItemResponse;
import com.unionsg.xaccounting.dto.prepayment.PrepaymentResponse;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.prepayment.Prepayment;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentAmortizationLine;
import com.unionsg.xaccounting.entity.prepayment.PrepaymentType;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentCounterpartyType;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentLineStatus;
import com.unionsg.xaccounting.enums.prepayment.PrepaymentStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeRepository;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentRepository;
import com.unionsg.xaccounting.repository.prepayment.PrepaymentTypeRepository;
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
 * Lifecycle for the prepaid-expense side of Prepayments (spec §7-11): create a draft with its
 * recognition schedule, activate it (posts the cash-out journal), recognize each period as it
 * falls due (posts that period's expense journal), and cancel/write-off as needed.
 *
 * <p>Deliberately out of scope this pass (documented, not silently dropped): schedule
 * restructuring, partial-amount recognition against a specific line, early termination with a
 * refund, and correction/extension adjustments (spec §10's adjustment types). A prepayment with
 * any period already recognized cannot be reversed - it can only be written off - since a true
 * reversal would need to unwind each individual recognition journal.</p>
 */
@Service
@RequiredArgsConstructor
public class PrepaymentService {

    private final PrepaymentRepository repository;
    private final PrepaymentTypeRepository typeRepository;
    private final SupplierRepository supplierRepository;
    private final EmployeeRepository employeeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final DocumentNumberService documentNumberService;
    private final PrepaymentJournalService journalService;

    @Transactional
    public PrepaymentResponse create(CreatePrepaymentRequest request) {
        if (request.getTotalAmount() == null || request.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Total amount must be positive");
        }
        if (request.getNumberOfPeriods() == null || request.getNumberOfPeriods() <= 0) {
            throw new BusinessException("Number of periods must be positive");
        }

        PrepaymentType type = typeRepository.findById(request.getPrepaymentTypeId())
                .orElseThrow(() -> new BusinessException("Prepayment type not found with ID: " + request.getPrepaymentTypeId()));

        Prepayment prepayment = new Prepayment();
        prepayment.setPrepaymentType(type);
        prepayment.setCounterpartyType(request.getCounterpartyType());
        resolveCounterparty(prepayment, request);

        prepayment.setTotalAmount(request.getTotalAmount());
        prepayment.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        prepayment.setPaymentDate(request.getPaymentDate());
        prepayment.setRecognitionStartDate(request.getRecognitionStartDate());
        prepayment.setNumberOfPeriods(request.getNumberOfPeriods());
        prepayment.setRecognitionFrequency(request.getRecognitionFrequency());
        prepayment.setAmountRecognized(BigDecimal.ZERO);
        prepayment.setAmountRemaining(request.getTotalAmount());
        prepayment.setNotes(request.getNotes());
        prepayment.setStatus(PrepaymentStatus.DRAFT);

        if (request.getPrepaidAccountId() != null) {
            prepayment.setPrepaidAccount(resolveAccount(request.getPrepaidAccountId()));
        }
        if (request.getExpenseAccountId() != null) {
            prepayment.setExpenseAccount(resolveAccount(request.getExpenseAccountId()));
        }
        if (request.getBankAccountId() != null) {
            prepayment.setBankAccount(bankAccountRepository.findById(request.getBankAccountId())
                    .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + request.getBankAccountId())));
        }

        prepayment.setPrepaymentNumber(documentNumberService.generateNextNumber(DocumentModule.PREPAYMENT));

        generateSchedule(prepayment);

        return PrepaymentMapper.toResponse(repository.save(prepayment));
    }

    private void resolveCounterparty(Prepayment prepayment, CreatePrepaymentRequest request) {
        if (request.getCounterpartyType() == PrepaymentCounterpartyType.SUPPLIER) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new BusinessException("Supplier not found with ID: " + request.getSupplierId()));
            prepayment.setSupplier(supplier);
            prepayment.setCounterpartyName(supplier.getDisplayName());
        } else if (request.getCounterpartyType() == PrepaymentCounterpartyType.EMPLOYEE) {
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new BusinessException("Employee not found with ID: " + request.getEmployeeId()));
            prepayment.setEmployee(employee);
            prepayment.setCounterpartyName(employee.getFullName());
        } else {
            if (request.getCounterpartyName() == null || request.getCounterpartyName().isBlank()) {
                throw new BusinessException("A counterparty name is required for an OTHER counterparty type");
            }
            prepayment.setCounterpartyName(request.getCounterpartyName());
        }
    }

    /**
     * Splits {@code totalAmount} evenly across {@code numberOfPeriods}, one period per
     * {@code recognitionFrequency} step starting at {@code recognitionStartDate}; the last
     * period absorbs any rounding remainder so the schedule always sums to exactly the total
     * (same rounding-remainder convention {@code InvoiceJournalService} already uses).
     */
    private void generateSchedule(Prepayment prepayment) {
        int periods = prepayment.getNumberOfPeriods();
        BigDecimal perPeriod = prepayment.getTotalAmount()
                .divide(BigDecimal.valueOf(periods), 2, RoundingMode.DOWN);

        BigDecimal allocated = BigDecimal.ZERO;
        LocalDate periodDate = prepayment.getRecognitionStartDate();
        List<PrepaymentAmortizationLine> schedule = new ArrayList<>();

        for (int i = 1; i <= periods; i++) {
            BigDecimal amount = (i == periods)
                    ? prepayment.getTotalAmount().subtract(allocated)
                    : perPeriod;
            allocated = allocated.add(amount);

            PrepaymentAmortizationLine line = new PrepaymentAmortizationLine();
            line.setPrepayment(prepayment);
            line.setPeriodNumber(i);
            line.setPeriodDate(periodDate);
            line.setAmount(amount);
            line.setStatus(PrepaymentLineStatus.PENDING);
            schedule.add(line);

            periodDate = advance(periodDate, prepayment.getRecognitionFrequency());
        }

        prepayment.setSchedule(schedule);
    }

    private LocalDate advance(LocalDate date, com.unionsg.xaccounting.enums.prepayment.PrepaymentFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> date.plusMonths(1);
            case QUARTERLY -> date.plusMonths(3);
            case SEMI_ANNUALLY -> date.plusMonths(6);
            case ANNUALLY -> date.plusYears(1);
        };
    }

    @Transactional
    public PrepaymentResponse activate(Long id) {
        Prepayment prepayment = loadPrepayment(id);
        if (prepayment.getStatus() != PrepaymentStatus.DRAFT) {
            throw new BusinessException("Only a draft prepayment can be activated");
        }

        JournalEntry journal = journalService.postActivationJournal(prepayment);
        prepayment.setJournal(journal);
        prepayment.setStatus(PrepaymentStatus.ACTIVE);
        prepayment.setActivatedAt(LocalDateTime.now());

        return PrepaymentMapper.toResponse(repository.save(prepayment));
    }

    /** Recognizes whichever schedule line is next in line (the lowest-numbered PENDING one). */
    @Transactional
    public PrepaymentResponse recognizeNext(Long id) {
        Prepayment prepayment = loadPrepayment(id);
        assertRecognizable(prepayment);

        PrepaymentAmortizationLine next = prepayment.getSchedule().stream()
                .filter(line -> line.getStatus() == PrepaymentLineStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Every period has already been recognized"));

        JournalEntry journal = journalService.postRecognitionJournal(prepayment, next);
        next.setJournal(journal);
        next.setStatus(PrepaymentLineStatus.RECOGNIZED);
        next.setRecognizedAt(LocalDateTime.now());

        prepayment.setAmountRecognized(prepayment.getAmountRecognized().add(next.getAmount()));
        prepayment.setAmountRemaining(prepayment.getAmountRemaining().subtract(next.getAmount()));

        boolean fullyRecognized = prepayment.getSchedule().stream()
                .allMatch(line -> line.getStatus() != PrepaymentLineStatus.PENDING);
        prepayment.setStatus(fullyRecognized ? PrepaymentStatus.FULLY_RECOGNIZED : PrepaymentStatus.PARTIALLY_RECOGNIZED);

        return PrepaymentMapper.toResponse(repository.save(prepayment));
    }

    private void assertRecognizable(Prepayment prepayment) {
        if (prepayment.getStatus() != PrepaymentStatus.ACTIVE
                && prepayment.getStatus() != PrepaymentStatus.PARTIALLY_RECOGNIZED) {
            throw new BusinessException("Only an active or partially recognized prepayment can be recognized further");
        }
    }

    @Transactional
    public PrepaymentResponse cancel(Long id) {
        Prepayment prepayment = loadPrepayment(id);
        if (prepayment.getStatus() != PrepaymentStatus.DRAFT) {
            throw new BusinessException("Only a draft prepayment can be cancelled - an activated one must be written off instead");
        }
        prepayment.setStatus(PrepaymentStatus.CANCELLED);
        prepayment.setCancelledAt(LocalDateTime.now());
        return PrepaymentMapper.toResponse(repository.save(prepayment));
    }

    /** Expenses the entire remaining balance in one journal and closes the record out. */
    @Transactional
    public PrepaymentResponse writeOff(Long id) {
        Prepayment prepayment = loadPrepayment(id);
        if (prepayment.getStatus() != PrepaymentStatus.ACTIVE
                && prepayment.getStatus() != PrepaymentStatus.PARTIALLY_RECOGNIZED) {
            throw new BusinessException("Only an active or partially recognized prepayment can be written off");
        }
        if (prepayment.getAmountRemaining().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("This prepayment has no remaining balance to write off");
        }

        BigDecimal remaining = prepayment.getAmountRemaining();
        journalService.postWriteOffJournal(prepayment, remaining, LocalDate.now());

        prepayment.getSchedule().stream()
                .filter(line -> line.getStatus() == PrepaymentLineStatus.PENDING)
                .forEach(line -> line.setStatus(PrepaymentLineStatus.SKIPPED));

        prepayment.setAmountRecognized(prepayment.getTotalAmount());
        prepayment.setAmountRemaining(BigDecimal.ZERO);
        prepayment.setStatus(PrepaymentStatus.WRITTEN_OFF);

        return PrepaymentMapper.toResponse(repository.save(prepayment));
    }

    @Transactional(readOnly = true)
    public PrepaymentResponse getById(Long id) {
        return PrepaymentMapper.toResponse(loadPrepayment(id));
    }

    @Transactional(readOnly = true)
    public Page<PrepaymentListItemResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(PrepaymentMapper::toListItemResponse);
    }

    private Prepayment loadPrepayment(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Prepayment not found with ID: " + id));
    }

    private AccountEntity resolveAccount(Long accountPk) {
        return accountRepository.findById(accountPk)
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + accountPk));
    }
}
