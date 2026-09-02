package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.dto.payment.AllocatePaymentRequest;
import com.unionsg.xaccounting.dto.payment.PaymentAllocationRequest;
import com.unionsg.xaccounting.dto.payment.PaymentAllocationResponse;
import com.unionsg.xaccounting.entity.invoice.Invoice;
import com.unionsg.xaccounting.entity.payment.PaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.PaymentEntity;
import com.unionsg.xaccounting.enums.InvoiceStatus;
import com.unionsg.xaccounting.enums.PaymentStatus;
import com.unionsg.xaccounting.exception.BusinessException;
import com.unionsg.xaccounting.repository.invoice.InvoiceRepository;
import com.unionsg.xaccounting.repository.payment.PaymentAllocationRepository;
import com.unionsg.xaccounting.repository.payment.PaymentRepository;
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
public class PaymentAllocationServiceImpl implements PaymentAllocationService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentJournalService paymentJournalService;

    // =========================================================================
    // ALLOCATE PAYMENT
    // =========================================================================

    @Override
    @Transactional
    public List<PaymentAllocationResponse> allocatePayment(Long paymentId, AllocatePaymentRequest request) {
        PaymentEntity payment = loadPayment(paymentId);
        validatePaymentForAllocation(payment);

        List<PaymentAllocationResponse> responses = new ArrayList<>();
        BigDecimal totalAllocated = payment.getAllocatedAmount() != null ? payment.getAllocatedAmount() : BigDecimal.ZERO;

        for (PaymentAllocationRequest allocReq : request.getAllocations()) {
            Invoice invoice = loadInvoice(allocReq.getInvoiceId());
            validateInvoiceForAllocation(invoice, payment);

            BigDecimal allocationAmount = allocReq.getAllocatedAmount();

            if (allocationAmount.compareTo(invoice.getBalance()) > 0) {
                throw new BusinessException(
                        "Allocation amount " + allocationAmount + " exceeds invoice " +
                                invoice.getInvoiceNumber() + " outstanding balance of " + invoice.getBalance()
                );
            }

            BigDecimal newTotalAllocated = totalAllocated.add(allocationAmount);
            if (newTotalAllocated.compareTo(payment.getAmountReceived()) > 0) {
                throw new BusinessException(
                        "Total allocation " + newTotalAllocated + " exceeds payment amount " + payment.getAmountReceived()
                );
            }

            BigDecimal outstandingBefore = invoice.getBalance();

            PaymentAllocationEntity allocation = PaymentAllocationEntity.builder()
                    .payment(payment)
                    .invoice(invoice)
                    .allocatedAmount(allocationAmount)
                    .notes(null)
                    .build();
            paymentAllocationRepository.save(allocation);

            updateInvoiceAfterAllocation(invoice, allocationAmount);

            totalAllocated = newTotalAllocated;

            responses.add(buildAllocationResponse(allocation, invoice, outstandingBefore));

            if (payment.getJournal() != null) {
                paymentJournalService.postAdditionalAllocationJournal(payment, allocation);
            }
        }

        updatePaymentTotals(payment);
        updatePaymentStatus(payment);
        paymentRepository.save(payment);

        log.info("Allocated {} amount(s) to payment ID: {}", request.getAllocations().size(), paymentId);
        return responses;
    }

    // =========================================================================
    // REMOVE ALLOCATION
    // =========================================================================

    @Override
    @Transactional
    public void removeAllocation(Long allocationId) {
        PaymentAllocationEntity allocation = paymentAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new BusinessException("Allocation not found with ID: " + allocationId));

        PaymentEntity payment = allocation.getPayment();
        Invoice invoice = allocation.getInvoice();
        BigDecimal allocationAmount = allocation.getAllocatedAmount();

        invoice.setAmountPaid(invoice.getAmountPaid().subtract(allocationAmount));
        invoice.setBalance(invoice.getBalance().add(allocationAmount));
        updateInvoiceStatus(invoice);
        invoiceRepository.save(invoice);

        payment.removeAllocation(allocation);

        if (payment.getJournal() != null) {
            paymentJournalService.postRemoveAllocationJournal(payment, allocation);
        }

        paymentAllocationRepository.delete(allocation);

        updatePaymentTotals(payment);
        updatePaymentStatus(payment);
        paymentRepository.save(payment);

        log.info("Removed allocation ID: {} from payment ID: {}", allocationId, payment.getId());
    }

    // =========================================================================
    // REALLOCATE PAYMENT
    // =========================================================================

    @Override
    @Transactional
    public List<PaymentAllocationResponse> reallocatePayment(Long allocationId, Long newInvoiceId, BigDecimal newAmount) {
        PaymentAllocationEntity allocation = paymentAllocationRepository.findById(allocationId)
                .orElseThrow(() -> new BusinessException("Allocation not found with ID: " + allocationId));

        PaymentEntity payment = allocation.getPayment();
        Invoice oldInvoice = allocation.getInvoice();

        BigDecimal oldAmount = allocation.getAllocatedAmount();
        oldInvoice.setAmountPaid(oldInvoice.getAmountPaid().subtract(oldAmount));
        oldInvoice.setBalance(oldInvoice.getBalance().add(oldAmount));
        updateInvoiceStatus(oldInvoice);
        invoiceRepository.save(oldInvoice);

        Invoice newInvoice = loadInvoice(newInvoiceId);
        validateInvoiceForAllocation(newInvoice, payment);

        if (newAmount.compareTo(newInvoice.getBalance()) > 0) {
            throw new BusinessException(
                    "Reallocation amount " + newAmount + " exceeds invoice " +
                            newInvoice.getInvoiceNumber() + " outstanding balance of " + newInvoice.getBalance()
            );
        }

        BigDecimal outstandingBefore = newInvoice.getBalance();
        allocation.setInvoice(newInvoice);
        allocation.setAllocatedAmount(newAmount);
        paymentAllocationRepository.save(allocation);

        updateInvoiceAfterAllocation(newInvoice, newAmount);

        updatePaymentTotals(payment);
        paymentRepository.save(payment);

        log.info("Reallocated allocation ID: {} from invoice {} to invoice {}",
                allocationId, oldInvoice.getInvoiceNumber(), newInvoice.getInvoiceNumber());

        return List.of(buildAllocationResponse(allocation, newInvoice, outstandingBefore));
    }

    // =========================================================================
    // AUTO ALLOCATE OLDEST
    // =========================================================================

    @Override
    @Transactional
    public List<PaymentAllocationResponse> autoAllocateOldest(Long paymentId) {
        PaymentEntity payment = loadPayment(paymentId);
        validatePaymentForAllocation(payment);

        List<Invoice> openInvoices = findOpenInvoicesForCustomer(payment.getCustomer().getId());

        openInvoices.sort(Comparator.comparing(Invoice::getIssueDate));

        return autoAllocate(payment, openInvoices);
    }

    // =========================================================================
    // AUTO ALLOCATE LARGEST
    // =========================================================================

    @Override
    @Transactional
    public List<PaymentAllocationResponse> autoAllocateLargest(Long paymentId) {
        PaymentEntity payment = loadPayment(paymentId);
        validatePaymentForAllocation(payment);

        List<Invoice> openInvoices = findOpenInvoicesForCustomer(payment.getCustomer().getId());

        openInvoices.sort(Comparator.comparing(Invoice::getBalance).reversed());

        return autoAllocate(payment, openInvoices);
    }

    // =========================================================================
    // CLEAR ALLOCATIONS
    // =========================================================================

    @Override
    @Transactional
    public void clearAllocations(Long paymentId) {
        PaymentEntity payment = loadPayment(paymentId);

        List<PaymentAllocationEntity> allocations = paymentAllocationRepository.findByPaymentId(paymentId);

        for (PaymentAllocationEntity allocation : allocations) {
            Invoice invoice = allocation.getInvoice();
            BigDecimal allocationAmount = allocation.getAllocatedAmount();

            invoice.setAmountPaid(invoice.getAmountPaid().subtract(allocationAmount));
            invoice.setBalance(invoice.getBalance().add(allocationAmount));
            updateInvoiceStatus(invoice);
            invoiceRepository.save(invoice);

            if (payment.getJournal() != null) {
                paymentJournalService.postRemoveAllocationJournal(payment, allocation);
            }

            paymentAllocationRepository.delete(allocation);
        }

        payment.setAllocatedAmount(BigDecimal.ZERO);
        payment.setUnallocatedAmount(payment.getAmountReceived());
        payment.setFullyAllocated(false);
        payment.setStatus(PaymentStatus.RECEIVED);
        paymentRepository.save(payment);

        log.info("Cleared all allocations for payment ID: {}", paymentId);
    }

    // =========================================================================
    // GET PAYMENT ALLOCATIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAllocationResponse> getPaymentAllocations(Long paymentId) {
        PaymentEntity payment = loadPayment(paymentId);

        return paymentAllocationRepository.findByPaymentId(paymentId).stream()
                .map(allocation -> {
                    Invoice invoice = allocation.getInvoice();
                    return buildAllocationResponse(allocation, invoice,
                            invoice.getBalance().add(allocation.getAllocatedAmount()));
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private PaymentEntity loadPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException("Payment not found with ID: " + paymentId));
    }

    private Invoice loadInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("Invoice not found with ID: " + invoiceId));
    }

    private void validatePaymentForAllocation(PaymentEntity payment) {
        if (payment.getStatus() == PaymentStatus.DRAFT) {
            throw new BusinessException("Cannot allocate a draft payment");
        }
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("Cannot allocate a refunded payment");
        }
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            throw new BusinessException("Cannot allocate a cancelled payment");
        }
    }

    private void validateInvoiceForAllocation(Invoice invoice, PaymentEntity payment) {
        if (!invoice.getCustomer().getId().equals(payment.getCustomer().getId())) {
            throw new BusinessException(
                    "Invoice " + invoice.getInvoiceNumber() + " belongs to a different customer"
            );
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BusinessException("Invoice " + invoice.getInvoiceNumber() + " is cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("Invoice " + invoice.getInvoiceNumber() + " is already fully paid");
        }
    }

    private void updateInvoiceAfterAllocation(Invoice invoice, BigDecimal allocationAmount) {
        invoice.setAmountPaid(invoice.getAmountPaid().add(allocationAmount));
        invoice.setBalance(invoice.getBalance().subtract(allocationAmount));
        updateInvoiceStatus(invoice);
        invoiceRepository.save(invoice);
    }

    private void updateInvoiceStatus(Invoice invoice) {
        InvoiceStatus newStatus;
        BigDecimal balance = invoice.getBalance();

        if (balance.compareTo(invoice.getTotalAmount()) == 0) {
            newStatus = InvoiceStatus.SENT;
        } else if (balance.compareTo(BigDecimal.ZERO) == 0) {
            newStatus = InvoiceStatus.PAID;
            invoice.setPaidAt(java.time.LocalDateTime.now());
        } else if (balance.compareTo(BigDecimal.ZERO) > 0
                && balance.compareTo(invoice.getTotalAmount()) < 0) {
            newStatus = InvoiceStatus.PARTIALLY_PAID;
        } else {
            newStatus = InvoiceStatus.SENT;
        }

        invoice.setStatus(newStatus);
    }

    private void updatePaymentTotals(PaymentEntity payment) {
        List<PaymentAllocationEntity> allocations = paymentAllocationRepository.findByPaymentId(payment.getId());

        BigDecimal allocatedAmount = allocations.stream()
                .map(PaymentAllocationEntity::getAllocatedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        payment.setAllocatedAmount(allocatedAmount);
        payment.setUnallocatedAmount(payment.getAmountReceived().subtract(allocatedAmount));
        payment.setFullyAllocated(allocatedAmount.compareTo(payment.getAmountReceived()) == 0);
    }

    private void updatePaymentStatus(PaymentEntity payment) {
        List<PaymentAllocationEntity> allocations = paymentAllocationRepository.findByPaymentId(payment.getId());

        if (allocations.isEmpty()) {
            payment.setStatus(PaymentStatus.RECEIVED);
        } else if (payment.getFullyAllocated() != null && payment.getFullyAllocated()) {
            payment.setStatus(PaymentStatus.ALLOCATED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_ALLOCATED);
        }
    }

    private PaymentAllocationResponse buildAllocationResponse(PaymentAllocationEntity allocation,
                                                               Invoice invoice,
                                                               BigDecimal outstandingBefore) {
        PaymentAllocationResponse response = new PaymentAllocationResponse();
        response.setAllocationId(allocation.getId());
        response.setInvoiceId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setInvoiceDate(invoice.getIssueDate());
        response.setInvoiceTotal(invoice.getTotalAmount());
        response.setOutstandingBefore(outstandingBefore);
        response.setAllocatedAmount(allocation.getAllocatedAmount());
        response.setOutstandingAfter(invoice.getBalance());
        return response;
    }

    private List<PaymentAllocationResponse> autoAllocate(PaymentEntity payment, List<Invoice> invoices) {
        List<PaymentAllocationResponse> responses = new ArrayList<>();
        BigDecimal remaining = payment.getAmountReceived().subtract(
                payment.getAllocatedAmount() != null ? payment.getAllocatedAmount() : BigDecimal.ZERO
        );

        for (Invoice invoice : invoices) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal outstanding = invoice.getBalance();
            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocationAmount = remaining.min(outstanding);
            BigDecimal outstandingBefore = invoice.getBalance();

            PaymentAllocationEntity allocation = PaymentAllocationEntity.builder()
                    .payment(payment)
                    .invoice(invoice)
                    .allocatedAmount(allocationAmount)
                    .notes("Auto-allocated")
                    .build();
            paymentAllocationRepository.save(allocation);

            updateInvoiceAfterAllocation(invoice, allocationAmount);

            remaining = remaining.subtract(allocationAmount);

            responses.add(buildAllocationResponse(allocation, invoice, outstandingBefore));

            if (payment.getJournal() != null) {
                paymentJournalService.postAdditionalAllocationJournal(payment, allocation);
            }
        }

        updatePaymentTotals(payment);
        updatePaymentStatus(payment);
        paymentRepository.save(payment);

        log.info("Auto-allocated {} invoices for payment ID: {}", responses.size(), payment.getId());
        return responses;
    }

    private List<Invoice> findOpenInvoicesForCustomer(Long customerId) {
        return invoiceRepository.findByCustomerId(customerId).stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED
                        && inv.getStatus() != InvoiceStatus.PAID
                        && inv.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
    }
}
