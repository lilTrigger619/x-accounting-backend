package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.MapperLayer.SupplierPaymentMapper;
import com.unionsg.xaccounting.dto.supplierpayment.CreateSupplierPaymentRequest;
import com.unionsg.xaccounting.dto.supplierpayment.CreateSupplierPaymentResponse;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentAllocationResponse;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentDetailsResponse;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentFilterRequest;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentListItemResponse;
import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentEntity;
import com.unionsg.xaccounting.entity.supplier.Supplier;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.payment.SupplierPaymentRepository;
import com.unionsg.xaccounting.utils.PaymentConstants;
import com.unionsg.xaccounting.validator.SupplierPaymentValidator;
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
public class SupplierPaymentServiceImpl implements SupplierPaymentService {

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierPaymentNumberGenerator numberGenerator;
    private final SupplierPaymentValidator validator;
    private final APJournalService apJournalService;

    // ========================================================================
    // CREATE PAYMENT
    // ========================================================================

    @Override
    @Transactional
    public CreateSupplierPaymentResponse createPayment(CreateSupplierPaymentRequest request) {
        validator.validatePositiveAmount(request.getAmountPaid(), "amountPaid");
        validator.validateCurrency(request.getCurrency());
        validator.validateExchangeRate(request.getExchangeRate());

        Supplier supplier = validator.validateSupplierExists(request.getSupplierId());
        ChartOfAccount bankAccount = null;
        if (request.getBankAccountId() != null) {
            bankAccount = validator.validateBankAccount(request.getBankAccountId());
        }

        SupplierPaymentEntity payment = SupplierPaymentMapper.toEntity(request, supplier, bankAccount);
        payment.setStatus(SupplierPaymentStatus.PAID);
        payment.setPaymentNumber(numberGenerator.generatePaymentNumber());

        SupplierPaymentEntity saved = supplierPaymentRepository.save(payment);

        apJournalService.postSupplierPaymentJournal(saved);

        return SupplierPaymentMapper.toCreateResponse(saved, "Payment created successfully");
    }

    // ========================================================================
    // SAVE DRAFT
    // ========================================================================

    @Override
    @Transactional
    public CreateSupplierPaymentResponse saveDraft(CreateSupplierPaymentRequest request) {
        validator.validatePositiveAmount(request.getAmountPaid(), "amountPaid");
        validator.validateCurrency(request.getCurrency());
        validator.validateExchangeRate(request.getExchangeRate());

        Supplier supplier = validator.validateSupplierExists(request.getSupplierId());
        ChartOfAccount bankAccount = null;
        if (request.getBankAccountId() != null) {
            bankAccount = validator.validateBankAccount(request.getBankAccountId());
        }

        SupplierPaymentEntity payment = SupplierPaymentMapper.toEntity(request, supplier, bankAccount);
        payment.setPaymentNumber(numberGenerator.generatePaymentNumber());

        SupplierPaymentEntity saved = supplierPaymentRepository.save(payment);

        return SupplierPaymentMapper.toCreateResponse(saved, "Draft payment saved successfully");
    }

    // ========================================================================
    // UPDATE DRAFT
    // ========================================================================

    @Override
    @Transactional
    public CreateSupplierPaymentResponse updateDraft(Long id, CreateSupplierPaymentRequest request) {
        SupplierPaymentEntity payment = loadPayment(id);
        validator.validateDraftPayment(payment);

        validator.validatePositiveAmount(request.getAmountPaid(), "amountPaid");
        validator.validateCurrency(request.getCurrency());
        validator.validateExchangeRate(request.getExchangeRate());

        Supplier supplier = validator.validateSupplierExists(request.getSupplierId());
        ChartOfAccount bankAccount = null;
        if (request.getBankAccountId() != null) {
            bankAccount = validator.validateBankAccount(request.getBankAccountId());
        }

        String existingNumber = payment.getPaymentNumber();

        payment.setSupplier(supplier);
        payment.setPaymentDate(request.getPaymentDate());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setBankAccount(bankAccount);
        payment.setCurrency(request.getCurrency());
        payment.setExchangeRate(request.getExchangeRate() != null ? request.getExchangeRate() : BigDecimal.ONE);
        payment.setAmountPaid(request.getAmountPaid());
        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setUnallocatedAmount(request.getAmountPaid());
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setMemo(request.getMemo());
        payment.setFullyAllocated(false);
        payment.getAllocations().clear();

        if (request.getAttachmentIds() != null) {
            payment.setAttachments(request.getAttachmentIds());
        }

        payment.setPaymentNumber(existingNumber);

        SupplierPaymentEntity saved = supplierPaymentRepository.save(payment);

        return SupplierPaymentMapper.toCreateResponse(saved, "Draft payment updated successfully");
    }

    // ========================================================================
    // DELETE DRAFT
    // ========================================================================

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        SupplierPaymentEntity payment = loadPayment(id);
        validator.validateDraftPayment(payment);
        payment.softDelete("system");
        supplierPaymentRepository.save(payment);
    }

    // ========================================================================
    // GET PAYMENT BY ID
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public SupplierPaymentDetailsResponse getPaymentById(Long id) {
        SupplierPaymentEntity payment = loadPayment(id);

        List<SupplierPaymentAllocationResponse> allocationResponses = payment.getAllocations().stream()
                .map(allocation -> {
                    BigDecimal outstandingBefore = BigDecimal.ZERO;
                    if (allocation.getBill() != null) {
                        outstandingBefore = allocation.getBill().getBalance().add(allocation.getAllocatedAmount());
                    }
                    return SupplierPaymentMapper.toAllocationResponse(allocation, outstandingBefore);
                })
                .collect(Collectors.toList());

        return SupplierPaymentMapper.toDetailsResponse(payment, allocationResponses);
    }

    // ========================================================================
    // LIST PAYMENTS
    // ========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierPaymentListItemResponse> getPayments(SupplierPaymentFilterRequest filterRequest) {
        Specification<SupplierPaymentEntity> specification = buildSearchSpecification(filterRequest);

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
            sort = Sort.by(Sort.Direction.DESC, "paymentDate");
        }

        int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
        int size = filterRequest.getSize() != null ? filterRequest.getSize() : 20;

        Pageable pageable = PageRequest.of(page, size, sort);

        return supplierPaymentRepository.findAll(specification, pageable)
                .map(SupplierPaymentMapper::toListItemResponse);
    }

    // ========================================================================
    // CHANGE STATUS
    // ========================================================================

    @Override
    @Transactional
    public void changeStatus(Long id, SupplierPaymentStatus newStatus) {
        SupplierPaymentEntity payment = loadPayment(id);
        payment.setStatus(newStatus);
        supplierPaymentRepository.save(payment);
    }

    // ========================================================================
    // PRIVATE HELPERS
    // ========================================================================

    private SupplierPaymentEntity loadPayment(Long id) {
        return supplierPaymentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Payment not found with ID: " + id));
    }

    private Specification<SupplierPaymentEntity> buildSearchSpecification(SupplierPaymentFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                Predicate numberPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("paymentNumber")), pattern);
                Predicate referencePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("referenceNumber")), pattern);
                Predicate memoPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("memo")), pattern);
                predicates.add(criteriaBuilder.or(numberPredicate, referencePredicate, memoPredicate));
            }

            if (filter.getSupplierId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("supplier").get("id"), filter.getSupplierId()));
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
}
