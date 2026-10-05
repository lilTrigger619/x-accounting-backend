package com.unionsg.xaccounting.service.downpayment;

import com.unionsg.xaccounting.MapperLayer.DownpaymentMapper;
import com.unionsg.xaccounting.dto.downpayment.AllocateDownpaymentRequest;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentAllocationResponse;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentRequest;
import com.unionsg.xaccounting.dto.downpayment.DownpaymentResponse;
import com.unionsg.xaccounting.dto.downpayment.OpenDocumentResponse;
import com.unionsg.xaccounting.dto.downpayment.RefundDownpaymentRequest;
import com.unionsg.xaccounting.entity.AccountEntity;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.downpayment.Downpayment;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentAllocation;
import com.unionsg.xaccounting.entity.downpayment.DownpaymentRefund;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.settings.BankAccount;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.enums.DocumentModule;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentStatus;
import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import com.unionsg.xaccounting.enums.settings.BankAccountStatus;
import com.unionsg.xaccounting.enums.settlement.SettlementDocumentType;
import com.unionsg.xaccounting.enums.settlement.SettlementSourceType;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.exception.ResourceNotFoundException;
import com.unionsg.xaccounting.repository.AccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.SupplierRepository;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.downpayment.DownpaymentAllocationRepository;
import com.unionsg.xaccounting.repository.downpayment.DownpaymentRefundRepository;
import com.unionsg.xaccounting.repository.downpayment.DownpaymentRepository;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.settings.BankAccountRepository;
import com.unionsg.xaccounting.service.DocumentNumberService;
import com.unionsg.xaccounting.service.accounting.PeriodLockGuard;
import com.unionsg.xaccounting.service.settlement.DocumentSettlementService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lifecycle of customer and supplier downpayments: draft, post, apply to invoices/bills
 * (full, partial, several at once), refund, and reverse any of those. Every movement goes
 * through {@link DownpaymentJournalService} (the journal engine) and every application
 * through the shared {@link DocumentSettlementService}, so invoices and bills keep a single
 * balance calculation shared with the Deposits module.
 */
@Service
@RequiredArgsConstructor
public class DownpaymentService {

    /** Statuses in which a downpayment still has money that can be applied or refunded. */
    public static final Set<DownpaymentStatus> USABLE = EnumSet.of(DownpaymentStatus.OPEN, DownpaymentStatus.PARTIALLY_APPLIED);

    private final DownpaymentRepository repository;
    private final DownpaymentAllocationRepository allocationRepository;
    private final DownpaymentRefundRepository refundRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillRepository billRepository;
    private final DocumentNumberService documentNumberService;
    private final DownpaymentJournalService journalService;
    private final DocumentSettlementService settlementService;
    private final PeriodLockGuard periodLockGuard;

    // =========================================================================
    // ENQUIRY
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<DownpaymentResponse> list(DownpaymentType type, DownpaymentStatus status, Long counterpartyId,
                                          String search, LocalDate from, LocalDate to, Boolean availableOnly,
                                          Pageable pageable) {
        Specification<Downpayment> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            if (type != null) p.add(cb.equal(root.get("type"), type));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (counterpartyId != null) {
                Predicate byCustomer = cb.equal(root.get("customer").get("id"), counterpartyId);
                Predicate bySupplier = cb.equal(root.get("supplier").get("id"), counterpartyId);
                if (type == DownpaymentType.CUSTOMER_DOWNPAYMENT) p.add(byCustomer);
                else if (type == DownpaymentType.SUPPLIER_DOWNPAYMENT) p.add(bySupplier);
                else p.add(cb.or(byCustomer, bySupplier));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("downpaymentNumber")), like),
                        cb.like(cb.lower(root.get("counterpartyName")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("reference"), "")), like)));
            }
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("paymentDate"), to));
            if (Boolean.TRUE.equals(availableOnly)) {
                p.add(root.get("status").in(USABLE));
                p.add(cb.greaterThan(root.get("availableBalance"), BigDecimal.ZERO));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, pageable).map(DownpaymentMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public DownpaymentResponse get(Long id) {
        return DownpaymentMapper.toResponse(load(id));
    }

    /** Usable downpayments of one customer/supplier, e.g. for the invoice/bill screen. */
    @Transactional(readOnly = true)
    public List<DownpaymentResponse> available(DownpaymentType type, Long counterpartyId, String currency) {
        return repository.findByTypeAndDeletedFalseAndStatusIn(type, USABLE).stream()
                .filter(dp -> counterpartyId == null || counterpartyId.equals(dp.getCounterpartyId()))
                .filter(dp -> currency == null || currency.equalsIgnoreCase(dp.getCurrency()))
                .filter(dp -> dp.getAvailableBalance().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(Downpayment::getPaymentDate).thenComparing(Downpayment::getId))
                .map(DownpaymentMapper::toSummary)
                .toList();
    }

    /** Invoices (customer) or bills (supplier) of the same counterparty and currency that still have a balance. */
    @Transactional(readOnly = true)
    public List<OpenDocumentResponse> openDocuments(Long id) {
        Downpayment dp = load(id);
        if (dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT) {
            return invoiceRepository.findByCustomerId(dp.getCustomer().getId()).stream()
                    .filter(i -> i.getStatus() != InvoiceStatus.CANCELLED && i.getStatus() != InvoiceStatus.DRAFT)
                    .filter(i -> nz(i.getBalance()).compareTo(BigDecimal.ZERO) > 0)
                    .filter(i -> sameCurrency(dp.getCurrency(), i.getCurrency()))
                    .sorted(Comparator.comparing(Invoice::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toOpenDocument)
                    .toList();
        }
        return billRepository.findBySupplierId(dp.getSupplier().getId()).stream()
                .filter(b -> b.getStatus() != BillStatus.CANCELLED && b.getStatus() != BillStatus.DRAFT)
                .filter(b -> nz(b.getBalance()).compareTo(BigDecimal.ZERO) > 0)
                .filter(b -> sameCurrency(dp.getCurrency(), b.getCurrency()))
                .sorted(Comparator.comparing(Bill::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toOpenDocument)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DownpaymentAllocationResponse> allocationsForInvoice(Long invoiceId) {
        return allocationRepository.findByInvoiceIdOrderByAllocationDateAscIdAsc(invoiceId).stream()
                .map(DownpaymentMapper::toAllocationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DownpaymentAllocationResponse> allocationsForBill(Long billId) {
        return allocationRepository.findByBillIdOrderByAllocationDateAscIdAsc(billId).stream()
                .map(DownpaymentMapper::toAllocationResponse).toList();
    }

    // =========================================================================
    // ORIGINATION / UPDATE / DELETE
    // =========================================================================

    @Transactional
    public DownpaymentResponse create(DownpaymentRequest request) {
        Downpayment dp = new Downpayment();
        apply(dp, request);
        dp.setStatus(DownpaymentStatus.DRAFT);
        dp.setAppliedAmount(BigDecimal.ZERO);
        dp.setRefundedAmount(BigDecimal.ZERO);
        dp.setAvailableBalance(dp.getAmount());
        dp.setDownpaymentNumber(documentNumberService.generateNextNumber(DocumentModule.DOWNPAYMENT));
        dp = repository.save(dp);
        if (Boolean.TRUE.equals(request.getPost())) {
            return post(dp.getId());
        }
        return DownpaymentMapper.toResponse(dp);
    }

    @Transactional
    public DownpaymentResponse update(Long id, DownpaymentRequest request) {
        Downpayment dp = load(id);
        if (dp.getStatus() != DownpaymentStatus.DRAFT) {
            throw new BusinessException("Only a draft downpayment can be edited. Reverse a posted one instead.");
        }
        if (request.getType() != null && request.getType() != dp.getType()) {
            throw new BusinessException("The downpayment type cannot be changed after creation");
        }
        request.setType(dp.getType());
        apply(dp, request);
        dp.setAvailableBalance(dp.getAmount());
        dp = repository.save(dp);
        if (Boolean.TRUE.equals(request.getPost())) {
            return post(dp.getId());
        }
        return DownpaymentMapper.toResponse(dp);
    }

    /** Soft-deletes a draft. Posted downpayments carry GL history and must be reversed instead. */
    @Transactional
    public void delete(Long id) {
        Downpayment dp = load(id);
        if (dp.getStatus() != DownpaymentStatus.DRAFT && dp.getStatus() != DownpaymentStatus.CANCELLED) {
            throw new BusinessException("Downpayment " + dp.getDownpaymentNumber()
                    + " has been posted and cannot be deleted. Reverse it instead.");
        }
        dp.setDeleted(true);
        dp.setDeletedAt(LocalDateTime.now());
        repository.save(dp);
    }

    @Transactional
    public DownpaymentResponse cancel(Long id) {
        Downpayment dp = load(id);
        if (dp.getStatus() != DownpaymentStatus.DRAFT) {
            throw new BusinessException("Only a draft downpayment can be cancelled. Reverse a posted one instead.");
        }
        dp.setStatus(DownpaymentStatus.CANCELLED);
        dp.setCancelledAt(LocalDateTime.now());
        dp.setAvailableBalance(BigDecimal.ZERO);
        return DownpaymentMapper.toResponse(repository.save(dp));
    }

    // =========================================================================
    // POSTING / REVERSAL
    // =========================================================================

    @Transactional
    public DownpaymentResponse post(Long id) {
        Downpayment dp = load(id);
        if (dp.getStatus() != DownpaymentStatus.DRAFT) {
            throw new BusinessException("Only a draft downpayment can be posted");
        }
        periodLockGuard.assertPostable(dp.getPaymentDate());
        dp.setJournal(journalService.postReceipt(dp));
        dp.setPostedAt(LocalDateTime.now());
        dp.setAvailableBalance(dp.getAmount());
        dp.setStatus(DownpaymentStatus.OPEN);
        return DownpaymentMapper.toResponse(repository.save(dp));
    }

    /** Reverses the receipt/payment journal. Allowed only once every application and refund is reversed. */
    @Transactional
    public DownpaymentResponse reverse(Long id, String reason) {
        Downpayment dp = load(id);
        if (dp.getJournal() == null || dp.getStatus() == DownpaymentStatus.REVERSED
                || dp.getStatus() == DownpaymentStatus.DRAFT || dp.getStatus() == DownpaymentStatus.CANCELLED) {
            throw new BusinessException("Only a posted downpayment can be reversed");
        }
        if (dp.getAppliedAmount().compareTo(BigDecimal.ZERO) > 0 || dp.getRefundedAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Downpayment " + dp.getDownpaymentNumber()
                    + " has been applied or refunded. Reverse those entries first.");
        }
        periodLockGuard.assertPostable(LocalDate.now());
        String why = reasonOr(reason, "Reversal of downpayment " + dp.getDownpaymentNumber());
        dp.setReversalJournal(journalService.reverse(dp, dp.getJournal(), why));
        dp.setReversedAt(LocalDateTime.now());
        dp.setReversalReason(why);
        dp.setAvailableBalance(BigDecimal.ZERO);
        dp.setStatus(DownpaymentStatus.REVERSED);
        return DownpaymentMapper.toResponse(repository.save(dp));
    }

    // =========================================================================
    // ALLOCATION
    // =========================================================================

    /**
     * Applies the downpayment to one or more invoices/bills. Everything is validated before any
     * journal is posted, and the whole request runs in one transaction, so it either applies
     * fully or not at all.
     */
    @Transactional
    public DownpaymentResponse allocate(Long id, AllocateDownpaymentRequest request) {
        Downpayment dp = load(id);
        assertUsable(dp);
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw new BusinessException("Choose at least one " + documentWord(dp) + " to apply the downpayment to");
        }
        LocalDate date = request.getAllocationDate() != null ? request.getAllocationDate() : LocalDate.now();
        if (date.isBefore(dp.getPaymentDate())) {
            throw new BusinessException("A downpayment cannot be applied before its payment date (" + dp.getPaymentDate() + ")");
        }
        periodLockGuard.assertPostable(date);

        Set<Long> seen = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (AllocateDownpaymentRequest.Line line : request.getLines()) {
            if (line.getDocumentId() == null) {
                throw new BusinessException("Each line must name the " + documentWord(dp) + " it applies to");
            }
            if (!seen.add(line.getDocumentId())) {
                throw new BusinessException("The same " + documentWord(dp) + " appears more than once in this allocation");
            }
            if (line.getAmount() == null || line.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Each amount applied must be greater than zero");
            }
            validateDocument(dp, line.getDocumentId(), line.getAmount());
            total = total.add(line.getAmount());
        }
        if (total.compareTo(dp.getAvailableBalance()) > 0) {
            throw new BusinessException("Total applied (" + total + ") exceeds the available balance of "
                    + dp.getAvailableBalance() + " on downpayment " + dp.getDownpaymentNumber());
        }

        for (AllocateDownpaymentRequest.Line line : request.getLines()) {
            DownpaymentAllocation allocation = new DownpaymentAllocation();
            allocation.setDownpayment(dp);
            allocation.setAmount(line.getAmount());
            allocation.setAllocationDate(date);
            allocation.setNotes(request.getNotes());
            allocation.setReversed(false);
            if (dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT) {
                allocation.setDocumentType(SettlementDocumentType.INVOICE);
                allocation.setInvoice(invoiceRepository.getReferenceById(line.getDocumentId()));
            } else {
                allocation.setDocumentType(SettlementDocumentType.BILL);
                allocation.setBill(billRepository.getReferenceById(line.getDocumentId()));
            }
            allocation = allocationRepository.save(allocation);

            if (dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT) {
                settlementService.settleInvoice(line.getDocumentId(), dp.getCustomer().getId(), dp.getCurrency(),
                        line.getAmount(), date, SettlementSourceType.DOWNPAYMENT, dp.getId(), allocation.getId(),
                        dp.getDownpaymentNumber());
            } else {
                settlementService.settleBill(line.getDocumentId(), dp.getSupplier().getId(), dp.getCurrency(),
                        line.getAmount(), date, SettlementSourceType.DOWNPAYMENT, dp.getId(), allocation.getId(),
                        dp.getDownpaymentNumber());
            }

            allocation.setJournal(journalService.postAllocation(dp, allocation));
            dp.getAllocations().add(allocationRepository.save(allocation));
        }

        recalculate(dp);
        return DownpaymentMapper.toResponse(repository.save(dp));
    }

    @Transactional
    public DownpaymentResponse reverseAllocation(Long id, Long allocationId, String reason) {
        Downpayment dp = load(id);
        DownpaymentAllocation allocation = allocationRepository.findById(allocationId)
                .filter(a -> a.getDownpayment().getId().equals(dp.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found on this downpayment"));
        if (Boolean.TRUE.equals(allocation.getReversed())) {
            throw new BusinessException("This allocation has already been reversed");
        }
        periodLockGuard.assertPostable(LocalDate.now());
        String why = reasonOr(reason, "Reversal of downpayment application to " + allocation.getDocumentNumber());

        settlementService.reverse(SettlementSourceType.DOWNPAYMENT, allocation.getId());
        allocation.setReversalJournal(journalService.reverse(dp, allocation.getJournal(), why));
        allocation.setReversed(true);
        allocation.setReversedAt(LocalDateTime.now());
        allocation.setReversalReason(why);
        allocationRepository.save(allocation);

        recalculate(dp);
        return DownpaymentMapper.toResponse(repository.save(dp));
    }

    // =========================================================================
    // REFUND
    // =========================================================================

    @Transactional
    public DownpaymentResponse refund(Long id, RefundDownpaymentRequest request) {
        Downpayment dp = load(id);
        assertUsable(dp);
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Refund amount must be greater than zero");
        }
        if (amount.compareTo(dp.getAvailableBalance()) > 0) {
            throw new BusinessException("Refund of " + amount + " exceeds the available balance of "
                    + dp.getAvailableBalance() + " on downpayment " + dp.getDownpaymentNumber());
        }
        LocalDate date = request.getRefundDate() != null ? request.getRefundDate() : LocalDate.now();
        if (date.isBefore(dp.getPaymentDate())) {
            throw new BusinessException("A refund cannot be dated before the downpayment (" + dp.getPaymentDate() + ")");
        }
        periodLockGuard.assertPostable(date);

        BankAccount bank = request.getBankAccountId() != null ? loadBankAccount(request.getBankAccountId()) : dp.getBankAccount();
        if (bank.getCurrency() != null && !sameCurrency(dp.getCurrency(), bank.getCurrency())) {
            throw new BusinessException("Bank account " + bank.getAccountName() + " is in " + bank.getCurrency()
                    + ", not " + dp.getCurrency());
        }

        DownpaymentRefund refund = new DownpaymentRefund();
        refund.setDownpayment(dp);
        refund.setAmount(amount);
        refund.setRefundDate(date);
        refund.setBankAccount(bank);
        refund.setReference(request.getReference());
        refund.setReason(request.getReason());
        refund.setReversed(false);
        refund.setRefundNumber(dp.getDownpaymentNumber() + "-R" + (refundRepository.countByDownpaymentId(dp.getId()) + 1));
        refund = refundRepository.save(refund);

        refund.setJournal(journalService.postRefund(dp, refund));
        dp.getRefunds().add(refundRepository.save(refund));

        recalculate(dp);
        return DownpaymentMapper.toResponse(repository.save(dp));
    }

    @Transactional
    public DownpaymentResponse reverseRefund(Long id, Long refundId, String reason) {
        Downpayment dp = load(id);
        DownpaymentRefund refund = refundRepository.findById(refundId)
                .filter(r -> r.getDownpayment().getId().equals(dp.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found on this downpayment"));
        if (Boolean.TRUE.equals(refund.getReversed())) {
            throw new BusinessException("This refund has already been reversed");
        }
        periodLockGuard.assertPostable(LocalDate.now());
        String why = reasonOr(reason, "Reversal of refund " + refund.getRefundNumber());
        refund.setReversalJournal(journalService.reverse(dp, refund.getJournal(), why));
        refund.setReversed(true);
        refund.setReversedAt(LocalDateTime.now());
        refund.setReversalReason(why);
        refundRepository.save(refund);

        recalculate(dp);
        return DownpaymentMapper.toResponse(repository.save(dp));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /** Re-derives applied/refunded/available and the status from the history; nothing is stored by hand. */
    private void recalculate(Downpayment dp) {
        BigDecimal applied = allocationRepository.findByDownpaymentIdOrderByAllocationDateAscIdAsc(dp.getId()).stream()
                .filter(a -> !Boolean.TRUE.equals(a.getReversed()))
                .map(DownpaymentAllocation::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunded = refundRepository.findByDownpaymentIdOrderByRefundDateAscIdAsc(dp.getId()).stream()
                .filter(r -> !Boolean.TRUE.equals(r.getReversed()))
                .map(DownpaymentRefund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal available = dp.getAmount().subtract(applied).subtract(refunded);
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Downpayment " + dp.getDownpaymentNumber() + " would be over-used");
        }
        dp.setAppliedAmount(applied);
        dp.setRefundedAmount(refunded);
        dp.setAvailableBalance(available);

        if (available.compareTo(dp.getAmount()) == 0) {
            dp.setStatus(DownpaymentStatus.OPEN);
        } else if (available.compareTo(BigDecimal.ZERO) > 0) {
            dp.setStatus(DownpaymentStatus.PARTIALLY_APPLIED);
        } else if (refunded.compareTo(BigDecimal.ZERO) == 0) {
            dp.setStatus(DownpaymentStatus.FULLY_APPLIED);
        } else if (applied.compareTo(BigDecimal.ZERO) == 0) {
            dp.setStatus(DownpaymentStatus.REFUNDED);
        } else {
            dp.setStatus(DownpaymentStatus.CLOSED);
        }
    }

    private void validateDocument(Downpayment dp, Long documentId, BigDecimal amount) {
        if (dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT) {
            settlementService.validateInvoiceSettlement(documentId, dp.getCustomer().getId(), dp.getCurrency(), amount);
        } else {
            settlementService.validateBillSettlement(documentId, dp.getSupplier().getId(), dp.getCurrency(), amount);
        }
    }

    private void apply(Downpayment dp, DownpaymentRequest request) {
        if (request.getType() == null) {
            throw new BusinessException("Choose whether this is a customer or a supplier downpayment");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than zero");
        }
        if (request.getPaymentDate() == null) {
            throw new BusinessException("Payment date is required");
        }
        if (request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new BusinessException("Currency is required");
        }
        if (request.getBankAccountId() == null) {
            throw new BusinessException("Choose the bank or cash account the money moved through");
        }
        dp.setType(request.getType());
        if (request.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT) {
            if (request.getCustomerId() == null) throw new BusinessException("Customer is required");
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new BusinessException("Customer not found with ID: " + request.getCustomerId()));
            dp.setCustomer(customer);
            dp.setSupplier(null);
            dp.setCounterpartyName(customerName(customer));
        } else {
            if (request.getSupplierId() == null) throw new BusinessException("Supplier is required");
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new BusinessException("Supplier not found with ID: " + request.getSupplierId()));
            dp.setSupplier(supplier);
            dp.setCustomer(null);
            dp.setCounterpartyName(supplier.getDisplayName() != null ? supplier.getDisplayName() : supplier.getCompanyName());
        }
        BankAccount bank = loadBankAccount(request.getBankAccountId());
        if (bank.getCurrency() != null && !sameCurrency(request.getCurrency(), bank.getCurrency())) {
            throw new BusinessException("Bank account " + bank.getAccountName() + " is in " + bank.getCurrency()
                    + ", not " + request.getCurrency());
        }
        dp.setBankAccount(bank);
        if (request.getControlAccountCode() != null && !request.getControlAccountCode().isBlank()) {
            AccountEntity account = accountRepository.findByAccountId(request.getControlAccountCode())
                    .orElseThrow(() -> new BusinessException("Account not found with code: " + request.getControlAccountCode()));
            dp.setControlAccount(account);
        } else {
            dp.setControlAccount(null);
        }
        dp.setPaymentDate(request.getPaymentDate());
        dp.setAmount(request.getAmount());
        dp.setCurrency(request.getCurrency().trim().toUpperCase());
        dp.setReference(request.getReference());
        dp.setDescription(request.getDescription());
    }

    private BankAccount loadBankAccount(Long id) {
        BankAccount bank = bankAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Bank account not found with ID: " + id));
        if (bank.getStatus() != null && bank.getStatus() != BankAccountStatus.ACTIVE) {
            throw new BusinessException("Bank account " + bank.getAccountName() + " is not active");
        }
        return bank;
    }

    private void assertUsable(Downpayment dp) {
        if (!USABLE.contains(dp.getStatus())) {
            throw new BusinessException("Downpayment " + dp.getDownpaymentNumber() + " is "
                    + dp.getStatus().getLabel().toLowerCase() + " and has nothing available to use");
        }
    }

    private Downpayment load(Long id) {
        return repository.findById(id)
                .filter(dp -> !Boolean.TRUE.equals(dp.getDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Downpayment not found with ID: " + id));
    }

    private OpenDocumentResponse toOpenDocument(Invoice i) {
        OpenDocumentResponse r = new OpenDocumentResponse();
        r.setDocumentType(SettlementDocumentType.INVOICE);
        r.setDocumentId(i.getId());
        r.setDocumentNumber(i.getInvoiceNumber());
        r.setReference(i.getReference());
        r.setDocumentDate(i.getIssueDate());
        r.setDueDate(i.getDueDate());
        r.setCurrency(i.getCurrency());
        r.setStatus(i.getStatus().name());
        r.setTotalAmount(i.getTotalAmount());
        r.setBalance(nz(i.getBalance()));
        return r;
    }

    private OpenDocumentResponse toOpenDocument(Bill b) {
        OpenDocumentResponse r = new OpenDocumentResponse();
        r.setDocumentType(SettlementDocumentType.BILL);
        r.setDocumentId(b.getId());
        r.setDocumentNumber(b.getBillNumber());
        r.setReference(b.getSupplierReference());
        r.setDocumentDate(b.getBillDate());
        r.setDueDate(b.getDueDate());
        r.setCurrency(b.getCurrency());
        r.setStatus(b.getStatus().name());
        r.setTotalAmount(b.getTotalAmount());
        r.setBalance(nz(b.getBalance()));
        return r;
    }

    static String customerName(Customer c) {
        if (c.getDisplayName() != null && !c.getDisplayName().isBlank()) return c.getDisplayName();
        if (c.getCompanyName() != null && !c.getCompanyName().isBlank()) return c.getCompanyName();
        return ((c.getFirstName() != null ? c.getFirstName() : "") + " " + (c.getLastName() != null ? c.getLastName() : "")).trim();
    }

    private static String documentWord(Downpayment dp) {
        return dp.getType() == DownpaymentType.CUSTOMER_DOWNPAYMENT ? "invoice" : "bill";
    }

    private static String reasonOr(String reason, String fallback) {
        return reason != null && !reason.isBlank() ? reason.trim() : fallback;
    }

    private static boolean sameCurrency(String a, String b) {
        return a == null || b == null || a.equalsIgnoreCase(b);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
