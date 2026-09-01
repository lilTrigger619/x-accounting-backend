package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.MapperLayer.SupplierPaymentMapper;
import com.unionsg.xaccounting.dto.supplierpayment.AllocateSupplierPaymentRequest;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentAllocationRequest;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentAllocationResponse;
import com.unionsg.xaccounting.entity.bill.Bill;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.SupplierPaymentEntity;
import com.unionsg.xaccounting.enums.BillStatus;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.bill.BillRepository;
import com.unionsg.xaccounting.repository.payment.SupplierPaymentAllocationRepository;
import com.unionsg.xaccounting.repository.payment.SupplierPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierPaymentAllocationServiceImpl implements SupplierPaymentAllocationService {

    private final SupplierPaymentRepository supplierPaymentRepository;
    private final SupplierPaymentAllocationRepository allocationRepository;
    private final BillRepository billRepository;
    private final APJournalService apJournalService;

    // =========================================================================
    // ALLOCATE PAYMENT
    // =========================================================================

    @Override
    @Transactional
    public List<SupplierPaymentAllocationResponse> allocatePayment(Long paymentId, AllocateSupplierPaymentRequest request) {
        SupplierPaymentEntity payment = loadPayment(paymentId);
        validatePaymentForAllocation(payment);

        List<SupplierPaymentAllocationResponse> responses = new ArrayList<>();
        BigDecimal totalAllocated = payment.getAllocatedAmount() != null ? payment.getAllocatedAmount() : BigDecimal.ZERO;

        for (SupplierPaymentAllocationRequest allocReq : request.getAllocations()) {
            Bill bill = loadBill(allocReq.getBillId());
            validateBillForAllocation(bill, payment);

            BigDecimal allocationAmount = allocReq.getAllocatedAmount();

            if (allocationAmount.compareTo(bill.getBalance()) > 0) {
                throw new BusinessException(
                        "Allocation amount " + allocationAmount + " exceeds bill " +
                                bill.getBillNumber() + " outstanding balance of " + bill.getBalance()
                );
            }

            BigDecimal newTotalAllocated = totalAllocated.add(allocationAmount);
            if (newTotalAllocated.compareTo(payment.getAmountPaid()) > 0) {
                throw new BusinessException(
                        "Total allocation " + newTotalAllocated + " exceeds payment amount " + payment.getAmountPaid()
                );
            }

            BigDecimal outstandingBefore = bill.getBalance();

            SupplierPaymentAllocationEntity allocation = SupplierPaymentAllocationEntity.builder()
                    .supplierPayment(payment)
                    .bill(bill)
                    .allocatedAmount(allocationAmount)
                    .notes(null)
                    .build();
            allocationRepository.save(allocation);

            updateBillAfterAllocation(bill, allocationAmount);

            totalAllocated = newTotalAllocated;

            responses.add(SupplierPaymentMapper.toAllocationResponse(allocation, outstandingBefore));

            if (payment.getJournal() != null) {
                apJournalService.postAdditionalAllocationJournal(payment, allocation);
            }
        }

        updatePaymentTotals(payment);
        updatePaymentStatus(payment);
        supplierPaymentRepository.save(payment);

        log.info("Allocated {} amount(s) to supplier payment ID: {}", request.getAllocations().size(), paymentId);
        return responses;
    }

    // =========================================================================
    // REMOVE ALLOCATION
    // =========================================================================

    @Override
    @Transactional
    public void removeAllocation(Long allocationId) {
        SupplierPaymentAllocationEntity allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new BusinessException("Allocation not found with ID: " + allocationId));

        SupplierPaymentEntity payment = allocation.getSupplierPayment();
        Bill bill = allocation.getBill();
        BigDecimal allocationAmount = allocation.getAllocatedAmount();

        bill.setAmountPaid(bill.getAmountPaid().subtract(allocationAmount));
        bill.setBalance(bill.getBalance().add(allocationAmount));
        updateBillStatus(bill);
        billRepository.save(bill);

        payment.removeAllocation(allocation);
        allocationRepository.delete(allocation);

        updatePaymentTotals(payment);
        updatePaymentStatus(payment);
        supplierPaymentRepository.save(payment);

        if (payment.getJournal() != null) {
            apJournalService.postRemoveAllocationJournal(payment, allocation);
        }

        log.info("Removed allocation ID: {} from supplier payment ID: {}", allocationId, payment.getId());
    }

    // =========================================================================
    // AUTO ALLOCATE OLDEST
    // =========================================================================

    @Override
    @Transactional
    public List<SupplierPaymentAllocationResponse> autoAllocateOldest(Long paymentId) {
        SupplierPaymentEntity payment = loadPayment(paymentId);
        validatePaymentForAllocation(payment);

        List<Bill> openBills = findOpenBillsForSupplier(payment.getSupplier().getId());
        openBills.sort(Comparator.comparing(Bill::getBillDate));

        return autoAllocate(payment, openBills);
    }

    // =========================================================================
    // AUTO ALLOCATE LARGEST
    // =========================================================================

    @Override
    @Transactional
    public List<SupplierPaymentAllocationResponse> autoAllocateLargest(Long paymentId) {
        SupplierPaymentEntity payment = loadPayment(paymentId);
        validatePaymentForAllocation(payment);

        List<Bill> openBills = findOpenBillsForSupplier(payment.getSupplier().getId());
        openBills.sort(Comparator.comparing(Bill::getBalance).reversed());

        return autoAllocate(payment, openBills);
    }

    // =========================================================================
    // CLEAR ALLOCATIONS
    // =========================================================================

    @Override
    @Transactional
    public void clearAllocations(Long paymentId) {
        SupplierPaymentEntity payment = loadPayment(paymentId);

        List<SupplierPaymentAllocationEntity> allocations = allocationRepository.findBySupplierPaymentId(paymentId);

        for (SupplierPaymentAllocationEntity allocation : allocations) {
            Bill bill = allocation.getBill();
            BigDecimal allocationAmount = allocation.getAllocatedAmount();

            bill.setAmountPaid(bill.getAmountPaid().subtract(allocationAmount));
            bill.setBalance(bill.getBalance().add(allocationAmount));
            updateBillStatus(bill);
            billRepository.save(bill);

            allocationRepository.delete(allocation);

            if (payment.getJournal() != null) {
                apJournalService.postRemoveAllocationJournal(payment, allocation);
            }
        }

        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setUnallocatedAmount(payment.getAmountPaid());
        payment.setFullyAllocated(false);
        payment.setStatus(SupplierPaymentStatus.PAID);
        supplierPaymentRepository.save(payment);

        log.info("Cleared all allocations for supplier payment ID: {}", paymentId);
    }

    // =========================================================================
    // GET PAYMENT ALLOCATIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SupplierPaymentAllocationResponse> getPaymentAllocations(Long paymentId) {
        loadPayment(paymentId);

        return allocationRepository.findBySupplierPaymentId(paymentId).stream()
                .map(allocation -> {
                    Bill bill = allocation.getBill();
                    return SupplierPaymentMapper.toAllocationResponse(allocation,
                            bill.getBalance().add(allocation.getAllocatedAmount()));
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private SupplierPaymentEntity loadPayment(Long paymentId) {
        return supplierPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found with ID: " + paymentId));
    }

    private Bill loadBill(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new BusinessException("Bill not found with ID: " + billId));
    }

    private void validatePaymentForAllocation(SupplierPaymentEntity payment) {
        if (payment.getStatus() == SupplierPaymentStatus.DRAFT) {
            throw new BusinessException("Cannot allocate a draft payment");
        }
        if (payment.getStatus() == SupplierPaymentStatus.CANCELLED) {
            throw new BusinessException("Cannot allocate a cancelled payment");
        }
    }

    private void validateBillForAllocation(Bill bill, SupplierPaymentEntity payment) {
        if (!bill.getSupplier().getId().equals(payment.getSupplier().getId())) {
            throw new BusinessException(
                    "Bill " + bill.getBillNumber() + " belongs to a different supplier"
            );
        }
        if (bill.getStatus() == BillStatus.CANCELLED || bill.getStatus() == BillStatus.DRAFT) {
            throw new BusinessException("Bill " + bill.getBillNumber() + " is not open for payment");
        }
        if (bill.getStatus() == BillStatus.PAID || bill.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("Bill " + bill.getBillNumber() + " is already fully paid");
        }
    }

    private void updateBillAfterAllocation(Bill bill, BigDecimal allocationAmount) {
        bill.setAmountPaid(bill.getAmountPaid().add(allocationAmount));
        bill.setBalance(bill.getBalance().subtract(allocationAmount));
        updateBillStatus(bill);
        billRepository.save(bill);
    }

    private void updateBillStatus(Bill bill) {
        BillStatus newStatus;
        BigDecimal balance = bill.getBalance();

        if (balance.compareTo(bill.getTotalAmount()) == 0) {
            newStatus = BillStatus.OPEN;
        } else if (balance.compareTo(BigDecimal.ZERO) == 0) {
            newStatus = BillStatus.PAID;
            bill.setPaidAt(java.time.LocalDateTime.now());
        } else if (balance.compareTo(BigDecimal.ZERO) > 0
                && balance.compareTo(bill.getTotalAmount()) < 0) {
            newStatus = BillStatus.PARTIALLY_PAID;
        } else {
            newStatus = BillStatus.OPEN;
        }

        bill.setStatus(newStatus);
    }

    private void updatePaymentTotals(SupplierPaymentEntity payment) {
        List<SupplierPaymentAllocationEntity> allocations = allocationRepository.findBySupplierPaymentId(payment.getId());

        BigDecimal allocatedAmount = allocations.stream()
                .map(SupplierPaymentAllocationEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        payment.setAllocatedAmount(allocatedAmount);
        payment.setUnallocatedAmount(payment.getAmountPaid().subtract(allocatedAmount));
        payment.setFullyAllocated(allocatedAmount.compareTo(payment.getAmountPaid()) == 0);
    }

    private void updatePaymentStatus(SupplierPaymentEntity payment) {
        List<SupplierPaymentAllocationEntity> allocations = allocationRepository.findBySupplierPaymentId(payment.getId());

        if (allocations.isEmpty()) {
            payment.setStatus(SupplierPaymentStatus.PAID);
        } else if (payment.getFullyAllocated() != null && payment.getFullyAllocated()) {
            payment.setStatus(SupplierPaymentStatus.ALLOCATED);
        } else {
            payment.setStatus(SupplierPaymentStatus.PARTIALLY_ALLOCATED);
        }
    }

    private List<SupplierPaymentAllocationResponse> autoAllocate(SupplierPaymentEntity payment, List<Bill> bills) {
        List<SupplierPaymentAllocationResponse> responses = new ArrayList<>();
        BigDecimal remaining = payment.getAmountPaid().subtract(
                payment.getAllocatedAmount() != null ? payment.getAllocatedAmount() : BigDecimal.ZERO
        );

        for (Bill bill : bills) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal outstanding = bill.getBalance();
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocationAmount = remaining.min(outstanding);
            BigDecimal outstandingBefore = bill.getBalance();

            SupplierPaymentAllocationEntity allocation = SupplierPaymentAllocationEntity.builder()
                    .supplierPayment(payment)
                    .bill(bill)
                    .allocatedAmount(allocationAmount)
                    .notes("Auto-allocated")
                    .build();
            allocationRepository.save(allocation);

            updateBillAfterAllocation(bill, allocationAmount);

            remaining = remaining.subtract(allocationAmount);

            responses.add(SupplierPaymentMapper.toAllocationResponse(allocation, outstandingBefore));

            if (payment.getJournal() != null) {
                apJournalService.postAdditionalAllocationJournal(payment, allocation);
            }
        }

        updatePaymentTotals(payment);
        updatePaymentStatus(payment);
        supplierPaymentRepository.save(payment);

        log.info("Auto-allocated {} bills for supplier payment ID: {}", responses.size(), payment.getId());
        return responses;
    }

    private List<Bill> findOpenBillsForSupplier(Long supplierId) {
        return billRepository.findBySupplierId(supplierId).stream()
                .filter(b -> b.getStatus() != BillStatus.CANCELLED
                        && b.getStatus() != BillStatus.DRAFT
                        && b.getStatus() != BillStatus.PAID
                        && b.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }
}
