package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.MapperLayer.PaymentMapper;
import com.unionsg.xaccounting.dto.payment.*;
import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.payment.PaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.PaymentEntity;
import com.unionsg.xaccounting.entity.payment.PaymentRefundEntity;
import com.unionsg.xaccounting.enums.PaymentStatus;
import com.unionsg.xaccounting.exception.BadRequestException;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.ChartOfAccountRepository;
import com.unionsg.xaccounting.repository.CustomerRepository;
import com.unionsg.xaccounting.repository.payment.PaymentAllocationRepository;
import com.unionsg.xaccounting.repository.payment.PaymentRefundRepository;
import com.unionsg.xaccounting.repository.payment.PaymentRepository;
import com.unionsg.xaccounting.utils.PaymentConstants;
import com.unionsg.xaccounting.validator.PaymentValidator;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final PaymentRefundRepository paymentRefundRepository;
    private final ReceiptNumberGenerator receiptNumberGenerator;
    private final PaymentValidator paymentValidator;
    private final CustomerRepository customerRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final PaymentJournalService paymentJournalService;

    // ========================================================================
    // CREATE PAYMENT
    // ========================================================================

    @Override
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        paymentValidator.validatePositiveAmount(request.getAmountReceived(), "amountReceived");
        paymentValidator.validateCurrency(request.getCurrency());
        paymentValidator.validateExchangeRate(request.getExchangeRate());

        Customer customer = paymentValidator.validateCustomerExists(request.getCustomerId());
        ChartOfAccount bankAccount = null;
        if (request.getBankAccountId() != null) {
            bankAccount = paymentValidator.validateBankAccount(request.getBankAccountId());
        }

        PaymentEntity payment = initializePayment(request, customer, bankAccount, PaymentStatus.RECEIVED);
        payment.setReceiptNumber(receiptNumberGenerator.generateReceiptNumber());

        PaymentEntity saved = paymentRepository.save(payment);

        paymentJournalService.postPaymentJournal(saved);

        return PaymentMapper.toCreateResponse(saved, "Payment created successfully");
    }

    // ========================================================================
    // SAVE DRAFT
    // ========================================================================

    @Override
    @Transactional
    public CreatePaymentResponse saveDraft(CreateDraftPaymentRequest request) {
        paymentValidator.validatePositiveAmount(request.getAmountReceived(), "amountReceived");
        paymentValidator.validateCurrency(request.getCurrency());
        paymentValidator.validateExchangeRate(request.getExchangeRate());

        Customer customer = paymentValidator.validateCustomerExists(request.getCustomerId());
        ChartOfAccount bankAccount = null;
        if (request.getBankAccountId() != null) {
            bankAccount = paymentValidator.validateBankAccount(request.getBankAccountId());
        }

        PaymentEntity payment = initializeDraftPayment(request, customer, bankAccount);
        payment.setReceiptNumber(receiptNumberGenerator.generateReceiptNumber());

        PaymentEntity saved = paymentRepository.save(payment);

        return PaymentMapper.toCreateResponse(saved, "Draft payment saved successfully");
    }

    // ========================================================================
    // UPDATE DRAFT
    // ========================================================================

    @Override
    @Transactional
    public CreatePaymentResponse updateDraft(Long id, UpdateDraftPaymentRequest request) {
        PaymentEntity payment = loadPayment(id);
        paymentValidator.validateDraftPayment(payment);

        paymentValidator.validatePositiveAmount(request.getAmountReceived(), "amountReceived");
        paymentValidator.validateCurrency(request.getCurrency());
        paymentValidator.validateExchangeRate(request.getExchangeRate());

        Customer customer = paymentValidator.validateCustomerExists(request.getCustomerId());
        ChartOfAccount bankAccount = null;
        if (request.getBankAccountId() != null) {
            bankAccount = paymentValidator.validateBankAccount(request.getBankAccountId());
        }

        String existingReceiptNumber = payment.getReceiptNumber();
        applyDraftUpdates(payment, request, customer, bankAccount);
        payment.setReceiptNumber(existingReceiptNumber);

        PaymentEntity saved = paymentRepository.save(payment);

        return PaymentMapper.toCreateResponse(saved, "Draft payment updated successfully");
    }

    // ========================================================================
    // DELETE DRAFT
    // ========================================================================

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        PaymentEntity payment = loadPayment(id);
        paymentValidator.validateDraftPayment(payment);
        payment.softDelete("system");
        paymentRepository.save(payment);
    }

    // ========================================================================
    // GET PAYMENT BY ID
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public PaymentDetailsResponse getPaymentById(Long id) {
        PaymentEntity payment = loadPayment(id);

        List<PaymentAllocationResponse> allocationResponses = payment.getAllocations().stream()
                .map(allocation -> {
                    BigDecimal outstandingBefore = BigDecimal.ZERO;
                    BigDecimal outstandingAfter = BigDecimal.ZERO;
                    if (allocation.getInvoice() != null) {
                        outstandingBefore = allocation.getInvoice().getTotalDue();
                        outstandingAfter = outstandingBefore.subtract(allocation.getAllocatedAmount())
                                .max(BigDecimal.ZERO);
                    }
                    return PaymentMapper.toAllocationResponse(allocation, outstandingBefore, outstandingAfter);
                })
                .collect(Collectors.toList());

        List<PaymentRefundResponse> refundResponses = payment.getRefunds().stream()
                .map(PaymentMapper::toRefundResponse)
                .collect(Collectors.toList());

        return PaymentMapper.toDetailsResponse(payment, allocationResponses, refundResponses);
    }

    // ========================================================================
    // LIST PAYMENTS
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentListItemResponse> getPayments(PaymentFilterRequest filterRequest) {
        Specification<PaymentEntity> specification = buildSearchSpecification(filterRequest);

        Sort sort;
        if (filterRequest.getSortBy() != null) {
            String direction = filterRequest.getSortDirection() != null
                    ? filterRequest.getSortDirection()
                    : PaymentConstants.DEFAULT_SORT_DIRECTION;
            sort = Sort.by(
                    direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
                    filterRequest.getSortBy()
            );
        } else {
            sort = Sort.by(Sort.Direction.DESC, PaymentConstants.DEFAULT_SORT_FIELD);
        }

        int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
        int size = filterRequest.getSize() != null ? filterRequest.getSize() : 20;

        Pageable pageable = PageRequest.of(page, size, sort);

        return paymentRepository.findAll(specification, pageable)
                .map(PaymentMapper::toListItemResponse);
    }

    // ========================================================================
    // CHANGE STATUS
    // ========================================================================

    @Override
    @Transactional
    public void changeStatus(Long id, PaymentStatus newStatus) {
        PaymentEntity payment = loadPayment(id);
        validateStatusTransition(payment.getStatus(), newStatus);
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private PaymentEntity loadPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Payment not found with ID: " + id));
    }

    private PaymentEntity initializePayment(
            CreatePaymentRequest request,
            Customer customer,
            ChartOfAccount bankAccount,
            PaymentStatus status
    ) {
        PaymentEntity payment = PaymentMapper.toEntity(
                convertToCreatePaymentRequest(request),
                customer,
                bankAccount
        );
        payment.setStatus(status);
        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setUnallocatedAmount(request.getAmountReceived());
        payment.setFullyAllocated(false);
        return payment;
    }

    private PaymentEntity initializeDraftPayment(
            CreateDraftPaymentRequest request,
            Customer customer,
            ChartOfAccount bankAccount
    ) {
        PaymentEntity payment = new PaymentEntity();
        payment.setCustomer(customer);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setBankAccount(bankAccount);
        payment.setCurrency(request.getCurrency());
        payment.setExchangeRate(request.getExchangeRate() != null
                ? request.getExchangeRate()
                : BigDecimal.ONE);
        payment.setAmountReceived(request.getAmountReceived());
        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setUnallocatedAmount(request.getAmountReceived());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setMemo(request.getMemo());
        payment.setStatus(PaymentStatus.DRAFT);
        payment.setFullyAllocated(false);

        if (request.getAttachmentIds() != null) {
            payment.setAttachments(request.getAttachmentIds());
        }

        return payment;
    }

    private void applyDraftUpdates(
            PaymentEntity payment,
            UpdateDraftPaymentRequest request,
            Customer customer,
            ChartOfAccount bankAccount
    ) {
        payment.setCustomer(customer);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setBankAccount(bankAccount);
        payment.setCurrency(request.getCurrency());
        payment.setExchangeRate(request.getExchangeRate() != null
                ? request.getExchangeRate()
                : BigDecimal.ONE);
        payment.setAmountReceived(request.getAmountReceived());
        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setUnallocatedAmount(request.getAmountReceived());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setMemo(request.getMemo());
        payment.setFullyAllocated(false);

        payment.getAllocations().clear();

        if (request.getAttachmentIds() != null) {
            payment.setAttachments(request.getAttachmentIds());
        }
    }

    private Specification<PaymentEntity> buildSearchSpecification(PaymentFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                Predicate receiptPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("receiptNumber")), pattern);
                Predicate referencePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("referenceNumber")), pattern);
                Predicate memoPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("memo")), pattern);
                predicates.add(criteriaBuilder.or(receiptPredicate, referencePredicate, memoPredicate));
            }

            if (filter.getCustomerId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("customer").get("id"), filter.getCustomerId()));
            }

            if (filter.getPaymentMethod() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("paymentMethod"), filter.getPaymentMethod()));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"), filter.getStatus()));
            }

            if (filter.getBankAccountId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("bankAccount").get("id"), filter.getBankAccountId()));
            }

            if (filter.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("paymentDate"), filter.getFromDate()));
            }

            if (filter.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("paymentDate"), filter.getToDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateStatusTransition(PaymentStatus current, PaymentStatus target) {
        if (current == target) {
            return;
        }

        switch (current) {
            case DRAFT:
                if (target == PaymentStatus.RECEIVED || target == PaymentStatus.CANCELLED) {
                    return;
                }
                break;
            case RECEIVED:
                if (target == PaymentStatus.PARTIALLY_ALLOCATED
                        || target == PaymentStatus.ALLOCATED
                        || target == PaymentStatus.CANCELLED) {
                    return;
                }
                break;
            case PARTIALLY_ALLOCATED:
                if (target == PaymentStatus.ALLOCATED
                        || target == PaymentStatus.REFUNDED
                        || target == PaymentStatus.CANCELLED) {
                    return;
                }
                break;
            case ALLOCATED:
                if (target == PaymentStatus.REFUNDED || target == PaymentStatus.CANCELLED) {
                    return;
                }
                break;
            case REFUNDED:
                if (target == PaymentStatus.CANCELLED) {
                    return;
                }
                break;
            case CANCELLED:
                break;
        }

        throw new BusinessException(
                "Invalid payment status transition from " + current + " to " + target);
    }

    private CreatePaymentRequest convertToCreatePaymentRequest(CreatePaymentRequest request) {
        return request;
    }
}
