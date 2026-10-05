package com.unionsg.xaccounting.service.deposit;

import com.unionsg.xaccounting.MapperLayer.DepositMapper;
import com.unionsg.xaccounting.dto.deposit.*;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.deposit.Deposit;
import com.unionsg.xaccounting.entity.deposit.DepositAllocation;
import com.unionsg.xaccounting.entity.deposit.DepositType;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.payroll.Employee;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.enums.deposit.*;
import com.unionsg.xaccounting.enums.settlement.SettlementSourceType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.deposit.DepositAllocationRepository;
import com.unionsg.xaccounting.repository.deposit.DepositRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.payroll.EmployeeRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.config.ConfigValueValidator;
import com.unionsg.xaccounting.service.settlement.DocumentSettlementService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Lifecycle of a deposit paid or received:
 * DRAFT -> (activate, posts the deposit) ACTIVE -> PARTIALLY_APPLIED -> FULLY_APPLIED / REFUNDED /
 * FORFEITED, with CANCELLED for a draft dropped before posting and REVERSED for an active deposit
 * whose posting is undone. Every movement out of the balance is a {@link DepositAllocation}
 * (apply, refund, forfeit, transfer) with its own journal; the balance can never go below zero.
 */
@Service
@RequiredArgsConstructor
public class DepositService {

    private static final Set<DepositStatus> OPEN = EnumSet.of(DepositStatus.ACTIVE, DepositStatus.PARTIALLY_APPLIED);

    private final DepositRepository repository;
    private final DepositAllocationRepository allocationRepository;
    private final DepositTypeService typeService;
    private final DepositJournalService journalService;
    private final DepositMapper mapper;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final EmployeeRepository employeeRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillRepository billRepository;
    private final DocumentNumberService documentNumberService;
    private final DocumentSettlementService settlementService;
    private final PeriodLockGuard periodLockGuard;
    private final ConfigValueValidator configValues;

    // ---------------------------------------------------------------- create / update / delete

    @Transactional
    public DepositResponse create(CreateDepositRequest request, boolean activate) {
        Deposit deposit = new Deposit();
        apply(deposit, request);
        deposit.setStatus(DepositStatus.DRAFT);
        deposit.setAvailableBalance(deposit.getAmount());
        deposit.setDepositNumber(documentNumberService.generateNextNumber(DocumentModule.DEPOSIT));
        deposit = repository.save(deposit);
        if (activate) {
            activateEntity(deposit);
        }
        return mapper.toResponse(deposit);
    }

    @Transactional
    public DepositResponse update(Long id, CreateDepositRequest request) {
        Deposit deposit = load(id);
        if (deposit.getStatus() != DepositStatus.DRAFT) {
            throw new BusinessException("Only a draft deposit can be edited. Use apply, refund, forfeit, transfer or reverse on a posted one.");
        }
        apply(deposit, request);
        deposit.setAvailableBalance(deposit.getAmount());
        return mapper.toResponse(repository.save(deposit));
    }

    /** Soft-deletes a draft or cancelled deposit; posted deposits must be reversed instead. */
    @Transactional
    public void delete(Long id) {
        Deposit deposit = load(id);
        if (deposit.getStatus() != DepositStatus.DRAFT && deposit.getStatus() != DepositStatus.CANCELLED) {
            throw new BusinessException("Deposit " + deposit.getDepositNumber() + " has been posted to the ledger and cannot be deleted. Reverse it instead.");
        }
        deposit.setDeleted(true);
        deposit.setDeletedAt(LocalDateTime.now());
        repository.save(deposit);
    }

    private void apply(Deposit deposit, CreateDepositRequest request) {
        if (request.getDirection() == null) {
            throw new BusinessException("Choose whether this deposit is paid or received");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("The deposit amount must be greater than zero");
        }
        if (request.getDepositDate() == null) {
            throw new BusinessException("A deposit date is required");
        }
        if (request.getExpectedReturnDate() != null && request.getExpectedReturnDate().isBefore(request.getDepositDate())) {
            throw new BusinessException("The expected return date cannot be before the deposit date");
        }
        if (request.getCounterpartyType() == null) {
            throw new BusinessException("A counterparty type is required");
        }

        DepositType type = typeService.getUsable(request.getDepositTypeId(), request.getDirection());
        deposit.setDirection(request.getDirection());
        deposit.setDepositType(type);
        resolveCounterparty(deposit, request.getCounterpartyType(), request.getCustomerId(),
                request.getSupplierId(), request.getEmployeeId(), request.getCounterpartyName());

        deposit.setAmount(request.getAmount().setScale(2, java.math.RoundingMode.HALF_UP));
        deposit.setCurrency(configValues.require("currencies", request.getCurrency(), deposit.getCurrency(), "Currency"));
        deposit.setDepositDate(request.getDepositDate());
        deposit.setExpectedReturnDate(request.getExpectedReturnDate());
        deposit.setRefundable(request.getRefundable() != null ? request.getRefundable() : type.getRefundableByDefault());
        deposit.setPurpose(trim(request.getPurpose(), 500, "Purpose"));
        deposit.setReference(trim(request.getReference(), 100, "Reference"));
        deposit.setNotes(blankToNull(request.getNotes()));

        deposit.setBankAccount(request.getBankAccountId() != null ? bankAccount(request.getBankAccountId()) : null);
        AccountEntity holding = request.getDepositAccountId() != null ? account(request.getDepositAccountId()) : null;
        DepositAccountRules.assertHoldingAccount(holding, request.getDirection());
        deposit.setDepositAccount(holding);

        boolean interest = request.getInterestBearing() != null ? request.getInterestBearing() : type.getInterestBearingByDefault();
        deposit.setInterestBearing(interest);
        if (interest) {
            if (request.getInterestRate() == null || request.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("An interest-bearing deposit needs an interest rate of zero or more");
            }
            deposit.setInterestRate(request.getInterestRate());
            deposit.setInterestStartDate(request.getInterestStartDate() != null ? request.getInterestStartDate() : request.getDepositDate());
            deposit.setInterestTerms(trim(request.getInterestTerms(), 500, "Interest terms"));
        } else {
            deposit.setInterestRate(null);
            deposit.setInterestStartDate(null);
            deposit.setInterestTerms(null);
        }
    }

    private void resolveCounterparty(Deposit deposit, DepositCounterpartyType type, Long customerId,
                                     Long supplierId, Long employeeId, String name) {
        deposit.setCounterpartyType(type);
        deposit.setCustomer(null);
        deposit.setSupplier(null);
        deposit.setEmployee(null);
        switch (type) {
            case CUSTOMER -> {
                if (customerId == null) throw new BusinessException("Choose the customer");
                Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new BusinessException("Customer not found with ID: " + customerId));
                deposit.setCustomer(customer);
                deposit.setCounterpartyName(customerName(customer));
            }
            case SUPPLIER -> {
                if (supplierId == null) throw new BusinessException("Choose the supplier");
                Supplier supplier = supplierRepository.findById(supplierId)
                        .orElseThrow(() -> new BusinessException("Supplier not found with ID: " + supplierId));
                deposit.setSupplier(supplier);
                deposit.setCounterpartyName(supplier.getDisplayName() != null ? supplier.getDisplayName() : supplier.getCompanyName());
            }
            case EMPLOYEE -> {
                if (employeeId == null) throw new BusinessException("Choose the employee");
                Employee employee = employeeRepository.findById(employeeId)
                        .orElseThrow(() -> new BusinessException("Employee not found with ID: " + employeeId));
                deposit.setEmployee(employee);
                deposit.setCounterpartyName(employee.getFullName());
            }
            default -> {
                if (name == null || name.isBlank()) {
                    throw new BusinessException("Enter the counterparty's name");
                }
                deposit.setCounterpartyName(trim(name, 200, "Counterparty name"));
            }
        }
    }

    // ---------------------------------------------------------------- lifecycle

    @Transactional
    public DepositResponse activate(Long id) {
        Deposit deposit = load(id);
        activateEntity(deposit);
        return mapper.toResponse(deposit);
    }

    private void activateEntity(Deposit deposit) {
        if (deposit.getStatus() != DepositStatus.DRAFT) {
            throw new BusinessException("Only a draft deposit can be activated");
        }
        periodLockGuard.assertPostable(deposit.getDepositDate());
        JournalEntry journal = journalService.postActivation(deposit);
        deposit.setJournal(journal);
        deposit.setStatus(DepositStatus.ACTIVE);
        deposit.setActivatedAt(LocalDateTime.now());
        deposit.setAvailableBalance(deposit.getAmount());
        repository.save(deposit);
    }

    @Transactional
    public DepositResponse cancel(Long id) {
        Deposit deposit = load(id);
        if (deposit.getStatus() != DepositStatus.DRAFT) {
            throw new BusinessException("Only a draft deposit can be cancelled. A posted deposit must be reversed instead.");
        }
        deposit.setStatus(DepositStatus.CANCELLED);
        deposit.setCancelledAt(LocalDateTime.now());
        deposit.setAvailableBalance(BigDecimal.ZERO);
        return mapper.toResponse(repository.save(deposit));
    }

    /**
     * Undoes the deposit's own posting. Only allowed while nothing has been taken out of it; any
     * application, refund, forfeiture or transfer must be reversed first. A deposit created by a
     * transfer is reversed by reversing that transfer on the source deposit.
     */
    @Transactional
    public DepositResponse reverse(Long id, String reason) {
        Deposit deposit = load(id);
        if (deposit.getStatus() != DepositStatus.ACTIVE) {
            throw new BusinessException(deposit.getStatus() == DepositStatus.PARTIALLY_APPLIED
                    ? "Reverse this deposit's applications, refunds, forfeitures and transfers first"
                    : "Only an active deposit with nothing applied can be reversed");
        }
        if (liveAllocations(deposit).findAny().isPresent()) {
            throw new BusinessException("Reverse this deposit's applications, refunds, forfeitures and transfers first");
        }
        String why = requireReason(reason);
        if (deposit.getTransferredFrom() != null) {
            DepositAllocation transfer = deposit.getTransferredFrom().getAllocations().stream()
                    .filter(a -> a.getAllocationType() == DepositAllocationType.TRANSFER && !Boolean.TRUE.equals(a.getReversed())
                            && a.getTargetDeposit() != null && a.getTargetDeposit().getId().equals(deposit.getId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("The transfer that created this deposit was not found"));
            reverseAllocationEntity(deposit.getTransferredFrom(), transfer, why);
            return mapper.toResponse(load(id));
        }
        JournalEntry reversal = journalService.reverse(deposit, deposit.getJournal(), "Reversal of deposit " + deposit.getDepositNumber() + ": " + why);
        deposit.setStatus(DepositStatus.REVERSED);
        deposit.setReversedAt(LocalDateTime.now());
        deposit.setReversalReason(why);
        deposit.setAvailableBalance(BigDecimal.ZERO);
        return mapper.toResponse(repository.save(deposit));
    }

    // ---------------------------------------------------------------- allocations

    /** Applies the deposit to one or more invoices (received) or bills (paid), fully or partially. */
    @Transactional
    public DepositResponse applyToDocuments(Long id, ApplyDepositRequest request) {
        Deposit deposit = load(id);
        assertOpen(deposit);
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException("Choose at least one " + documentWord(deposit) + " to apply the deposit to");
        }
        LocalDate date = allocationDate(deposit, request.getAllocationDate());
        boolean received = deposit.getDirection() == DepositDirection.DEPOSIT_RECEIVED;
        if (received && deposit.getCustomer() == null) {
            throw new BusinessException("Only a deposit received from a customer can be applied to invoices");
        }
        if (!received && deposit.getSupplier() == null) {
            throw new BusinessException("Only a deposit paid to a supplier can be applied to bills");
        }

        BigDecimal total = BigDecimal.ZERO;
        Set<Long> seen = new HashSet<>();
        for (ApplyDepositLineRequest line : request.getLines()) {
            Long docId = received ? line.getInvoiceId() : line.getBillId();
            if (docId == null) {
                throw new BusinessException("Each line needs " + (received ? "an invoice" : "a bill"));
            }
            if (!seen.add(docId)) {
                throw new BusinessException("The same " + documentWord(deposit) + " is listed twice");
            }
            positive(line.getAmount());
            total = total.add(line.getAmount());
            if (received) {
                settlementService.validateInvoiceSettlement(docId, deposit.getCustomer().getId(), deposit.getCurrency(), line.getAmount());
            } else {
                settlementService.validateBillSettlement(docId, deposit.getSupplier().getId(), deposit.getCurrency(), line.getAmount());
            }
        }
        assertWithinBalance(deposit, total);

        int sequence = deposit.getAllocations().size();
        for (ApplyDepositLineRequest line : request.getLines()) {
            DepositAllocation allocation = newAllocation(deposit, DepositAllocationType.APPLICATION, line.getAmount(), date,
                    request.getReference(), request.getNotes());
            String documentNumber;
            if (received) {
                Invoice invoice = invoiceRepository.findById(line.getInvoiceId()).orElseThrow();
                allocation.setInvoice(invoice);
                documentNumber = invoice.getInvoiceNumber();
            } else {
                Bill bill = billRepository.findById(line.getBillId()).orElseThrow();
                allocation.setBill(bill);
                documentNumber = bill.getBillNumber();
            }
            allocation = allocationRepository.save(allocation);
            deposit.getAllocations().add(allocation);

            if (received) {
                settlementService.settleInvoice(line.getInvoiceId(), deposit.getCustomer().getId(), deposit.getCurrency(),
                        line.getAmount(), date, SettlementSourceType.DEPOSIT, deposit.getId(), allocation.getId(), deposit.getDepositNumber());
            } else {
                settlementService.settleBill(line.getBillId(), deposit.getSupplier().getId(), deposit.getCurrency(),
                        line.getAmount(), date, SettlementSourceType.DEPOSIT, deposit.getId(), allocation.getId(), deposit.getDepositNumber());
            }
            allocation.setJournal(journalService.postApplication(deposit, line.getAmount(), date, documentNumber, "-A" + (++sequence)));
            allocationRepository.save(allocation);
        }

        recalculate(deposit);
        return mapper.toResponse(repository.save(deposit));
    }

    @Transactional
    public DepositResponse refund(Long id, RefundDepositRequest request) {
        Deposit deposit = load(id);
        assertOpen(deposit);
        if (!Boolean.TRUE.equals(deposit.getRefundable())) {
            throw new BusinessException("Deposit " + deposit.getDepositNumber() + " is non-refundable. Apply it or forfeit it instead.");
        }
        positive(request.getAmount());
        assertWithinBalance(deposit, request.getAmount());
        LocalDate date = allocationDate(deposit, request.getRefundDate());

        BankAccount bank = request.getBankAccountId() != null ? bankAccount(request.getBankAccountId()) : deposit.getBankAccount();
        DepositAllocation allocation = newAllocation(deposit, DepositAllocationType.REFUND, request.getAmount(), date,
                request.getReference(), request.getNotes());
        allocation.setBankAccount(bank);
        allocation = allocationRepository.save(allocation);
        deposit.getAllocations().add(allocation);
        allocation.setJournal(journalService.postRefund(deposit, request.getAmount(), date, bank, "-R" + deposit.getAllocations().size()));
        allocationRepository.save(allocation);

        recalculate(deposit);
        return mapper.toResponse(repository.save(deposit));
    }

    @Transactional
    public DepositResponse forfeit(Long id, ForfeitDepositRequest request) {
        Deposit deposit = load(id);
        assertOpen(deposit);
        positive(request.getAmount());
        assertWithinBalance(deposit, request.getAmount());
        if (request.getNotes() == null || request.getNotes().isBlank()) {
            throw new BusinessException("Give a reason for the forfeiture");
        }
        LocalDate date = allocationDate(deposit, request.getForfeitDate());
        AccountEntity override = request.getForfeitureAccountId() != null ? account(request.getForfeitureAccountId()) : null;
        DepositAccountRules.assertForfeitureAccount(override, deposit.getDirection());

        DepositAllocation allocation = newAllocation(deposit, DepositAllocationType.FORFEITURE, request.getAmount(), date,
                request.getReference(), request.getNotes());
        allocation = allocationRepository.save(allocation);
        deposit.getAllocations().add(allocation);
        allocation.setJournal(journalService.postForfeiture(deposit, request.getAmount(), date, override, "-F" + deposit.getAllocations().size()));
        allocationRepository.save(allocation);

        recalculate(deposit);
        return mapper.toResponse(repository.save(deposit));
    }

    /**
     * Moves part of the balance into a new, already-active deposit (same direction), e.g. to
     * another counterparty, another deposit type, or another holding account.
     */
    @Transactional
    public DepositResponse transfer(Long id, TransferDepositRequest request) {
        Deposit source = load(id);
        assertOpen(source);
        positive(request.getAmount());
        assertWithinBalance(source, request.getAmount());
        LocalDate date = allocationDate(source, request.getTransferDate());

        Deposit target = new Deposit();
        DepositType type = request.getDepositTypeId() != null
                ? typeService.getUsable(request.getDepositTypeId(), source.getDirection())
                : source.getDepositType();
        target.setDirection(source.getDirection());
        target.setDepositType(type);
        DepositCounterpartyType counterpartyType = request.getCounterpartyType() != null ? request.getCounterpartyType() : source.getCounterpartyType();
        if (request.getCounterpartyType() == null) {
            resolveCounterparty(target, counterpartyType,
                    source.getCustomer() != null ? source.getCustomer().getId() : null,
                    source.getSupplier() != null ? source.getSupplier().getId() : null,
                    source.getEmployee() != null ? source.getEmployee().getId() : null,
                    source.getCounterpartyName());
        } else {
            resolveCounterparty(target, counterpartyType, request.getCustomerId(), request.getSupplierId(),
                    request.getEmployeeId(), request.getCounterpartyName());
        }
        target.setAmount(request.getAmount());
        target.setCurrency(source.getCurrency());
        target.setDepositDate(date);
        target.setExpectedReturnDate(request.getExpectedReturnDate() != null ? request.getExpectedReturnDate() : source.getExpectedReturnDate());
        target.setRefundable(source.getRefundable());
        target.setPurpose(source.getPurpose());
        target.setReference(trim(request.getReference(), 100, "Reference"));
        target.setNotes(blankToNull(request.getNotes()));
        target.setBankAccount(source.getBankAccount());
        AccountEntity holding = request.getDepositAccountId() != null ? account(request.getDepositAccountId()) : null;
        DepositAccountRules.assertHoldingAccount(holding, source.getDirection());
        target.setDepositAccount(holding);
        target.setInterestBearing(source.getInterestBearing());
        target.setInterestRate(source.getInterestRate());
        target.setInterestStartDate(Boolean.TRUE.equals(source.getInterestBearing()) ? date : null);
        target.setInterestTerms(source.getInterestTerms());
        target.setTransferredFrom(source);
        target.setStatus(DepositStatus.ACTIVE);
        target.setActivatedAt(LocalDateTime.now());
        target.setAvailableBalance(request.getAmount());
        target.setDepositNumber(documentNumberService.generateNextNumber(DocumentModule.DEPOSIT));
        target = repository.save(target);

        DepositAllocation allocation = newAllocation(source, DepositAllocationType.TRANSFER, request.getAmount(), date,
                request.getReference(), request.getNotes());
        allocation.setTargetDeposit(target);
        allocation = allocationRepository.save(allocation);
        source.getAllocations().add(allocation);
        JournalEntry journal = journalService.postTransfer(source, target, request.getAmount(), date, "-T" + source.getAllocations().size());
        allocation.setJournal(journal);
        allocationRepository.save(allocation);
        target.setJournal(journal);
        repository.save(target);

        recalculate(source);
        return mapper.toResponse(repository.save(source));
    }

    /** Reverses one allocation: posts the offsetting journal, restores the balance, and keeps the row. */
    @Transactional
    public DepositResponse reverseAllocation(Long depositId, Long allocationId, String reason) {
        Deposit deposit = load(depositId);
        DepositAllocation allocation = deposit.getAllocations().stream()
                .filter(a -> a.getId().equals(allocationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found on deposit " + deposit.getDepositNumber()));
        reverseAllocationEntity(deposit, allocation, requireReason(reason));
        return mapper.toResponse(load(depositId));
    }

    private void reverseAllocationEntity(Deposit deposit, DepositAllocation allocation, String reason) {
        if (Boolean.TRUE.equals(allocation.getReversed())) {
            throw new BusinessException("This allocation has already been reversed");
        }
        if (deposit.getStatus() == DepositStatus.REVERSED || deposit.getStatus() == DepositStatus.CANCELLED) {
            throw new BusinessException("Deposit " + deposit.getDepositNumber() + " is " + deposit.getStatus().getLabel().toLowerCase());
        }
        periodLockGuard.assertPostable(LocalDate.now());

        if (allocation.getAllocationType() == DepositAllocationType.APPLICATION) {
            settlementService.reverse(SettlementSourceType.DEPOSIT, allocation.getId());
        } else if (allocation.getAllocationType() == DepositAllocationType.TRANSFER) {
            Deposit target = allocation.getTargetDeposit();
            if (target != null) {
                if (target.getStatus() != DepositStatus.ACTIVE || liveAllocations(target).findAny().isPresent()) {
                    throw new BusinessException("Deposit " + target.getDepositNumber()
                            + " created by this transfer has already been used. Reverse its allocations first.");
                }
                target.setStatus(DepositStatus.REVERSED);
                target.setReversedAt(LocalDateTime.now());
                target.setReversalReason("Transfer from " + deposit.getDepositNumber() + " reversed: " + reason);
                target.setAvailableBalance(BigDecimal.ZERO);
                repository.save(target);
            }
        }

        if (allocation.getJournal() != null) {
            allocation.setReversalJournal(journalService.reverse(deposit, allocation.getJournal(),
                    "Reversal of " + allocation.getAllocationType().getLabel().toLowerCase() + " on deposit "
                            + deposit.getDepositNumber() + ": " + reason));
        }
        allocation.setReversed(true);
        allocation.setReversedAt(LocalDateTime.now());
        allocation.setReversalReason(reason);
        allocationRepository.save(allocation);

        recalculate(deposit);
        repository.save(deposit);
    }

    // ---------------------------------------------------------------- reads

    @Transactional(readOnly = true)
    public DepositResponse get(Long id) {
        return mapper.toResponse(load(id));
    }

    @Transactional(readOnly = true)
    public Page<DepositListItemResponse> list(
            DepositDirection direction, DepositStatus status, DepositCounterpartyType counterpartyType,
            Long customerId, Long supplierId, Long employeeId, Long depositTypeId, String currency,
            String search, LocalDate fromDate, LocalDate toDate, Boolean openOnly, Pageable pageable
    ) {
        Specification<Deposit> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            if (direction != null) p.add(cb.equal(root.get("direction"), direction));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (Boolean.TRUE.equals(openOnly)) p.add(root.get("status").in(OPEN));
            if (counterpartyType != null) p.add(cb.equal(root.get("counterpartyType"), counterpartyType));
            if (customerId != null) p.add(cb.equal(root.get("customer").get("id"), customerId));
            if (supplierId != null) p.add(cb.equal(root.get("supplier").get("id"), supplierId));
            if (employeeId != null) p.add(cb.equal(root.get("employee").get("id"), employeeId));
            if (depositTypeId != null) p.add(cb.equal(root.get("depositType").get("id"), depositTypeId));
            if (currency != null && !currency.isBlank()) p.add(cb.equal(root.get("currency"), currency));
            if (fromDate != null) p.add(cb.greaterThanOrEqualTo(root.get("depositDate"), fromDate));
            if (toDate != null) p.add(cb.lessThanOrEqualTo(root.get("depositDate"), toDate));
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("depositNumber")), like),
                        cb.like(cb.lower(root.get("counterpartyName")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("reference"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("purpose"), "")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable).map(DepositMapper::toListItem);
    }

    /** Open invoices (deposit received) or bills (deposit paid) of the deposit's counterparty. */
    @Transactional(readOnly = true)
    public List<DepositOpenDocumentResponse> openDocuments(Long id) {
        Deposit deposit = load(id);
        List<DepositOpenDocumentResponse> result = new ArrayList<>();
        if (deposit.getDirection() == DepositDirection.DEPOSIT_RECEIVED && deposit.getCustomer() != null) {
            invoiceRepository.findByCustomerId(deposit.getCustomer().getId()).stream()
                    .filter(i -> i.getStatus() != InvoiceStatus.DRAFT && i.getStatus() != InvoiceStatus.CANCELLED
                            && i.getBalance() != null && i.getBalance().compareTo(BigDecimal.ZERO) > 0
                            && sameCurrency(deposit, i.getCurrency()))
                    .sorted(Comparator.comparing(Invoice::getIssueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(i -> result.add(openDoc("INVOICE", i.getId(), i.getInvoiceNumber(), i.getIssueDate(), i.getDueDate(),
                            i.getCurrency(), i.getStatus().name(), i.getTotalAmount(), i.getBalance())));
        } else if (deposit.getDirection() == DepositDirection.DEPOSIT_PAID && deposit.getSupplier() != null) {
            billRepository.findBySupplierId(deposit.getSupplier().getId()).stream()
                    .filter(b -> b.getStatus() != BillStatus.DRAFT && b.getStatus() != BillStatus.CANCELLED
                            && b.getBalance() != null && b.getBalance().compareTo(BigDecimal.ZERO) > 0
                            && sameCurrency(deposit, b.getCurrency()))
                    .sorted(Comparator.comparing(Bill::getBillDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(b -> result.add(openDoc("BILL", b.getId(), b.getBillNumber(), b.getBillDate(), b.getDueDate(),
                            b.getCurrency(), b.getStatus().name(), b.getTotalAmount(), b.getBalance())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public DepositDashboardResponse dashboard() {
        List<Deposit> deposits = repository.findByDeletedFalse();
        DepositDashboardResponse dashboard = new DepositDashboardResponse();
        for (DepositStatus status : DepositStatus.values()) {
            dashboard.getCountByStatus().put(status.name(), 0L);
        }
        LocalDate today = LocalDate.now();
        List<Deposit> overdue = new ArrayList<>();
        List<Deposit> upcoming = new ArrayList<>();
        for (Deposit d : deposits) {
            dashboard.getCountByStatus().merge(d.getStatus().name(), 1L, Long::sum);
            if (d.getStatus() == DepositStatus.DRAFT || d.getStatus() == DepositStatus.CANCELLED || d.getStatus() == DepositStatus.REVERSED) {
                continue;
            }
            Map<String, DepositTotalsResponse> byDirection = dashboard.getTotalsByCurrency()
                    .computeIfAbsent(d.getCurrency(), c -> {
                        Map<String, DepositTotalsResponse> m = new LinkedHashMap<>();
                        for (DepositDirection dir : DepositDirection.values()) m.put(dir.name(), new DepositTotalsResponse());
                        return m;
                    });
            DepositTotalsResponse t = byDirection.get(d.getDirection().name());
            t.setCount(t.getCount() + 1);
            if (OPEN.contains(d.getStatus())) t.setOpenCount(t.getOpenCount() + 1);
            t.setOriginalAmount(t.getOriginalAmount().add(d.getAmount()));
            t.setAppliedAmount(t.getAppliedAmount().add(d.getAppliedAmount()));
            t.setRefundedAmount(t.getRefundedAmount().add(d.getRefundedAmount()));
            t.setForfeitedAmount(t.getForfeitedAmount().add(d.getForfeitedAmount()));
            t.setTransferredAmount(t.getTransferredAmount().add(d.getTransferredAmount()));
            t.setAvailableBalance(t.getAvailableBalance().add(d.getAvailableBalance()));

            if (OPEN.contains(d.getStatus()) && d.getExpectedReturnDate() != null) {
                if (d.getExpectedReturnDate().isBefore(today)) overdue.add(d);
                else if (!d.getExpectedReturnDate().isAfter(today.plusDays(30))) upcoming.add(d);
            }
        }
        overdue.sort(Comparator.comparing(Deposit::getExpectedReturnDate));
        upcoming.sort(Comparator.comparing(Deposit::getExpectedReturnDate));
        overdue.stream().limit(20).map(DepositMapper::toListItem).forEach(dashboard.getOverdueReturns()::add);
        upcoming.stream().limit(20).map(DepositMapper::toListItem).forEach(dashboard.getUpcomingReturns()::add);
        dashboard.setRecentActivity(activityOf(deposits).stream().limit(10).toList());
        return dashboard;
    }

    @Transactional(readOnly = true)
    public Page<DepositActivityResponse> activity(DepositDirection direction, String event, Long depositId,
                                                   LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        List<Deposit> deposits = depositId != null ? List.of(load(depositId)) : repository.findByDeletedFalse();
        List<DepositActivityResponse> all = activityOf(deposits).stream()
                .filter(a -> direction == null || a.getDirection() == direction)
                .filter(a -> event == null || event.isBlank() || event.equals(a.getEvent()))
                .filter(a -> fromDate == null || !a.getDate().isBefore(fromDate))
                .filter(a -> toDate == null || !a.getDate().isAfter(toDate))
                .toList();
        int start = (int) Math.min(pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    /**
     * Running balance of one deposit, or of every deposit with one counterparty (in one direction
     * and currency), between two dates.
     */
    @Transactional(readOnly = true)
    public DepositStatementResponse statement(Long depositId, DepositCounterpartyType counterpartyType, Long customerId,
                                              Long supplierId, Long employeeId, String counterpartyName,
                                              DepositDirection direction, String currency,
                                              LocalDate fromDate, LocalDate toDate) {
        List<Deposit> deposits;
        DepositStatementResponse statement = new DepositStatementResponse();
        if (depositId != null) {
            Deposit d = load(depositId);
            deposits = List.of(d);
            statement.setTitle("Deposit " + d.getDepositNumber());
            statement.setCounterpartyName(d.getCounterpartyName());
            direction = d.getDirection();
        } else {
            if (customerId != null) {
                deposits = repository.findByCustomerIdAndDeletedFalseOrderByDepositDateAscIdAsc(customerId);
            } else if (supplierId != null) {
                deposits = repository.findBySupplierIdAndDeletedFalseOrderByDepositDateAscIdAsc(supplierId);
            } else if (employeeId != null) {
                deposits = repository.findByEmployeeIdAndDeletedFalseOrderByDepositDateAscIdAsc(employeeId);
            } else if (counterpartyName != null && !counterpartyName.isBlank()) {
                deposits = repository.findByCounterpartyTypeAndCounterpartyNameIgnoreCaseAndDeletedFalseOrderByDepositDateAscIdAsc(
                        counterpartyType != null ? counterpartyType : DepositCounterpartyType.OTHER, counterpartyName.trim());
            } else {
                throw new BusinessException("Choose a deposit or a counterparty for the statement");
            }
            statement.setCounterpartyName(deposits.isEmpty() ? counterpartyName : deposits.get(0).getCounterpartyName());
            statement.setTitle("Deposit statement");
        }
        DepositDirection dir = direction;
        deposits = deposits.stream().filter(d -> dir == null || d.getDirection() == dir).toList();

        List<String> currencies = deposits.stream().map(Deposit::getCurrency).distinct().sorted().toList();
        statement.setAvailableCurrencies(new ArrayList<>(currencies));
        String cur = currency != null && !currency.isBlank() ? currency : (currencies.isEmpty() ? null : currencies.get(0));
        statement.setCurrency(cur);
        statement.setDirection(dir);
        statement.setFromDate(fromDate);
        statement.setToDate(toDate);

        List<Event> events = new ArrayList<>();
        deposits.stream().filter(d -> Objects.equals(d.getCurrency(), cur)).forEach(d -> events.addAll(eventsOf(d)));
        events.removeIf(e -> e.delta.signum() == 0);
        events.sort(Comparator.comparing((Event e) -> e.date).thenComparing(e -> e.timestamp, Comparator.nullsLast(Comparator.naturalOrder())));

        BigDecimal balance = BigDecimal.ZERO;
        for (Event e : events) {
            if (fromDate != null && e.date.isBefore(fromDate)) {
                balance = balance.add(e.delta);
                continue;
            }
            if (toDate != null && e.date.isAfter(toDate)) {
                continue;
            }
            if (statement.getLines().isEmpty()) {
                statement.setOpeningBalance(balance);
            }
            balance = balance.add(e.delta);
            DepositStatementLineResponse line = new DepositStatementLineResponse();
            line.setDate(e.date);
            line.setDepositId(e.deposit.getId());
            line.setDepositNumber(e.deposit.getDepositNumber());
            line.setEvent(e.event);
            line.setDescription(e.description);
            line.setReference(e.reference);
            line.setIncrease(e.delta.signum() > 0 ? e.delta : BigDecimal.ZERO);
            line.setDecrease(e.delta.signum() < 0 ? e.delta.negate() : BigDecimal.ZERO);
            line.setBalance(balance);
            statement.getLines().add(line);
            statement.setTotalIncrease(statement.getTotalIncrease().add(line.getIncrease()));
            statement.setTotalDecrease(statement.getTotalDecrease().add(line.getDecrease()));
        }
        if (statement.getLines().isEmpty()) {
            statement.setOpeningBalance(balance);
        }
        statement.setClosingBalance(statement.getOpeningBalance().add(statement.getTotalIncrease()).subtract(statement.getTotalDecrease()));
        return statement;
    }

    // ---------------------------------------------------------------- events (statement + activity)

    private record Event(Deposit deposit, LocalDate date, LocalDateTime timestamp, String event, String label,
                         String description, String reference, BigDecimal delta, BigDecimal amount,
                         Long allocationId, String journalNumber) {
    }

    private List<Event> eventsOf(Deposit d) {
        List<Event> events = new ArrayList<>();
        String party = d.getCounterpartyName();
        events.add(new Event(d, d.getCreatedAt() != null ? d.getCreatedAt().toLocalDate() : d.getDepositDate(), d.getCreatedAt(),
                "CREATED", "Created", "Deposit recorded as a draft (" + party + ")", d.getReference(),
                BigDecimal.ZERO, d.getAmount(), null, null));
        if (d.getActivatedAt() != null) {
            boolean transferIn = d.getTransferredFrom() != null;
            events.add(new Event(d, d.getDepositDate(), d.getActivatedAt(),
                    transferIn ? "TRANSFER_IN" : "ACTIVATED", transferIn ? "Transferred In" : "Activated",
                    transferIn ? "Transferred in from " + d.getTransferredFrom().getDepositNumber()
                            : (d.getDirection() == DepositDirection.DEPOSIT_PAID ? "Deposit paid to " : "Deposit received from ") + party,
                    d.getReference(), d.getAmount(), d.getAmount(), null,
                    d.getJournal() != null ? d.getJournal().getJournalNumber() : null));
        }
        for (DepositAllocation a : d.getAllocations()) {
            events.add(new Event(d, a.getAllocationDate(), a.getCreatedAt(), a.getAllocationType().name(),
                    a.getAllocationType().getLabel(), describe(a), a.getReference(), a.getAmount().negate(), a.getAmount(),
                    a.getId(), a.getJournal() != null ? a.getJournal().getJournalNumber() : null));
            if (Boolean.TRUE.equals(a.getReversed()) && a.getReversedAt() != null) {
                events.add(new Event(d, a.getReversedAt().toLocalDate(), a.getReversedAt(), "ALLOCATION_REVERSED",
                        "Allocation Reversed", "Reversed: " + describe(a) + " (" + a.getReversalReason() + ")",
                        a.getReference(), a.getAmount(), a.getAmount(), a.getId(),
                        a.getReversalJournal() != null ? a.getReversalJournal().getJournalNumber() : null));
            }
        }
        if (d.getStatus() == DepositStatus.CANCELLED && d.getCancelledAt() != null) {
            events.add(new Event(d, d.getCancelledAt().toLocalDate(), d.getCancelledAt(), "CANCELLED", "Cancelled",
                    "Draft deposit cancelled", d.getReference(), BigDecimal.ZERO, d.getAmount(), null, null));
        }
        if (d.getStatus() == DepositStatus.REVERSED && d.getReversedAt() != null && d.getActivatedAt() != null) {
            events.add(new Event(d, d.getReversedAt().toLocalDate(), d.getReversedAt(), "REVERSED", "Reversed",
                    "Deposit reversed: " + d.getReversalReason(), d.getReference(), d.getAmount().negate(), d.getAmount(), null, null));
        }
        return events;
    }

    private List<DepositActivityResponse> activityOf(List<Deposit> deposits) {
        List<Event> events = new ArrayList<>();
        deposits.forEach(d -> events.addAll(eventsOf(d)));
        events.sort(Comparator.comparing((Event e) -> e.timestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(e -> e.date).reversed());
        return events.stream().map(e -> {
            DepositActivityResponse r = new DepositActivityResponse();
            r.setDate(e.date);
            r.setTimestamp(e.timestamp);
            r.setEvent(e.event);
            r.setEventLabel(e.label);
            r.setDepositId(e.deposit.getId());
            r.setDepositNumber(e.deposit.getDepositNumber());
            r.setDirection(e.deposit.getDirection());
            r.setCounterpartyName(e.deposit.getCounterpartyName());
            r.setCurrency(e.deposit.getCurrency());
            r.setAmount(e.amount);
            r.setAllocationId(e.allocationId);
            r.setDescription(e.description);
            r.setJournalNumber(e.journalNumber);
            return r;
        }).toList();
    }

    private static String describe(DepositAllocation a) {
        return switch (a.getAllocationType()) {
            case APPLICATION -> a.getInvoice() != null ? "Applied to invoice " + a.getInvoice().getInvoiceNumber()
                    : a.getBill() != null ? "Applied to bill " + a.getBill().getBillNumber() : "Applied";
            case REFUND -> "Refunded" + (a.getNotes() != null ? ": " + a.getNotes() : "");
            case FORFEITURE -> "Forfeited" + (a.getNotes() != null ? ": " + a.getNotes() : "");
            case TRANSFER -> "Transferred to " + (a.getTargetDeposit() != null
                    ? a.getTargetDeposit().getDepositNumber() + " (" + a.getTargetDeposit().getCounterpartyName() + ")" : "another deposit");
        };
    }

    // ---------------------------------------------------------------- helpers

    /** Recomputes the bucket totals and status from the non-reversed allocations. */
    private void recalculate(Deposit deposit) {
        BigDecimal applied = sum(deposit, DepositAllocationType.APPLICATION);
        BigDecimal refunded = sum(deposit, DepositAllocationType.REFUND);
        BigDecimal forfeited = sum(deposit, DepositAllocationType.FORFEITURE);
        BigDecimal transferred = sum(deposit, DepositAllocationType.TRANSFER);
        BigDecimal available = deposit.getAmount().subtract(applied).subtract(refunded).subtract(forfeited).subtract(transferred);
        if (available.signum() < 0) {
            throw new BusinessException("Deposit " + deposit.getDepositNumber() + " would be overdrawn");
        }
        deposit.setAppliedAmount(applied);
        deposit.setRefundedAmount(refunded);
        deposit.setForfeitedAmount(forfeited);
        deposit.setTransferredAmount(transferred);
        deposit.setAvailableBalance(available);

        if (available.compareTo(deposit.getAmount()) == 0) {
            deposit.setStatus(DepositStatus.ACTIVE);
        } else if (available.signum() > 0) {
            deposit.setStatus(DepositStatus.PARTIALLY_APPLIED);
        } else if (refunded.compareTo(deposit.getAmount()) == 0) {
            deposit.setStatus(DepositStatus.REFUNDED);
        } else if (forfeited.compareTo(deposit.getAmount()) == 0) {
            deposit.setStatus(DepositStatus.FORFEITED);
        } else {
            deposit.setStatus(DepositStatus.FULLY_APPLIED);
        }
    }

    private BigDecimal sum(Deposit deposit, DepositAllocationType type) {
        return liveAllocations(deposit).filter(a -> a.getAllocationType() == type)
                .map(DepositAllocation::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static java.util.stream.Stream<DepositAllocation> liveAllocations(Deposit deposit) {
        return deposit.getAllocations().stream().filter(a -> !Boolean.TRUE.equals(a.getReversed()));
    }

    private DepositAllocation newAllocation(Deposit deposit, DepositAllocationType type, BigDecimal amount,
                                            LocalDate date, String reference, String notes) {
        DepositAllocation allocation = new DepositAllocation();
        allocation.setDeposit(deposit);
        allocation.setAllocationType(type);
        allocation.setAmount(amount);
        allocation.setAllocationDate(date);
        allocation.setReference(trim(reference, 100, "Reference"));
        allocation.setNotes(trim(notes, 1000, "Notes"));
        allocation.setReversed(false);
        return allocation;
    }

    private void assertOpen(Deposit deposit) {
        if (!OPEN.contains(deposit.getStatus())) {
            throw new BusinessException("Deposit " + deposit.getDepositNumber() + " is "
                    + deposit.getStatus().getLabel().toLowerCase() + ". Only an active or partially applied deposit has a balance to use.");
        }
    }

    private static void assertWithinBalance(Deposit deposit, BigDecimal amount) {
        if (amount.compareTo(deposit.getAvailableBalance()) > 0) {
            throw new BusinessException("Amount " + amount + " exceeds the available deposit balance of "
                    + deposit.getAvailableBalance() + " " + deposit.getCurrency());
        }
    }

    private static void positive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("The amount must be greater than zero");
        }
        if (amount.scale() > 2 && amount.stripTrailingZeros().scale() > 2) {
            throw new BusinessException("The amount can have at most two decimal places");
        }
    }

    private LocalDate allocationDate(Deposit deposit, LocalDate requested) {
        LocalDate date = requested != null ? requested : LocalDate.now();
        if (date.isBefore(deposit.getDepositDate())) {
            throw new BusinessException("The date cannot be before the deposit date (" + deposit.getDepositDate() + ")");
        }
        periodLockGuard.assertPostable(date);
        return date;
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("Give a reason for the reversal");
        }
        return reason.trim().length() > 500 ? reason.trim().substring(0, 500) : reason.trim();
    }

    private static String documentWord(Deposit deposit) {
        return deposit.getDirection() == DepositDirection.DEPOSIT_RECEIVED ? "invoice" : "bill";
    }

    private static boolean sameCurrency(Deposit deposit, String currency) {
        return currency == null || deposit.getCurrency() == null || deposit.getCurrency().equalsIgnoreCase(currency);
    }

    private static DepositOpenDocumentResponse openDoc(String type, Long id, String number, LocalDate date, LocalDate due,
                                                       String currency, String status, BigDecimal total, BigDecimal balance) {
        DepositOpenDocumentResponse r = new DepositOpenDocumentResponse();
        r.setDocumentType(type);
        r.setId(id);
        r.setNumber(number);
        r.setDate(date);
        r.setDueDate(due);
        r.setCurrency(currency);
        r.setStatus(status);
        r.setTotalAmount(total);
        r.setBalance(balance);
        return r;
    }

    private Deposit load(Long id) {
        return repository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Deposit not found with ID: " + id));
    }

    private BankAccount bankAccount(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + id));
    }

    private AccountEntity account(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Account not found with ID: " + id));
    }

    private static String customerName(Customer customer) {
        if (customer.getDisplayName() != null && !customer.getDisplayName().isBlank()) return customer.getDisplayName();
        if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) return customer.getCompanyName();
        return ((customer.getFirstName() != null ? customer.getFirstName() : "") + " "
                + (customer.getLastName() != null ? customer.getLastName() : "")).trim();
    }

    private static String trim(String value, int max, String label) {
        if (value == null || value.isBlank()) return null;
        String t = value.trim();
        if (t.length() > max) {
            throw new BusinessException(label + " can be at most " + max + " characters");
        }
        return t;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
