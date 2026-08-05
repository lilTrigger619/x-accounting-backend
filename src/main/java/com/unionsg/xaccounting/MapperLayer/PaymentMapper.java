package com.unionsg.xaccounting.MapperLayer;

import com.unionsg.xaccounting.dto.payment.*;
import com.unionsg.xaccounting.entity.ChartOfAccount;
import com.unionsg.xaccounting.entity.Journals.JournalEntry;
import com.unionsg.xaccounting.entity.customer.Customer;
import com.unionsg.xaccounting.entity.payment.PaymentAllocationEntity;
import com.unionsg.xaccounting.entity.payment.PaymentEntity;
import com.unionsg.xaccounting.entity.payment.PaymentRefundEntity;
import com.unionsg.xaccounting.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentMapper {

    private PaymentMapper() {
        throw new UnsupportedOperationException("Mapper class cannot be instantiated");
    }

    public static PaymentEntity toEntity(
            CreatePaymentRequest request,
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

        if (request.getAllocations() != null && !request.getAllocations().isEmpty()) {
            List<PaymentAllocationEntity> allocations = request.getAllocations().stream()
                    .map(req -> {
                        PaymentAllocationEntity allocation = new PaymentAllocationEntity();
                        allocation.setPayment(payment);
                        allocation.setAllocatedAmount(req.getAllocatedAmount());
                        return allocation;
                    })
                    .collect(Collectors.toList());
            payment.setAllocations(allocations);
        }

        return payment;
    }

    public static void applyDraftUpdate(
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
        payment.setReferenceNumber(request.getReferenceNumber());
        payment.setMemo(request.getMemo());

        payment.getAllocations().clear();
        if (request.getAllocations() != null && !request.getAllocations().isEmpty()) {
            List<PaymentAllocationEntity> allocations = request.getAllocations().stream()
                    .map(req -> {
                        PaymentAllocationEntity allocation = new PaymentAllocationEntity();
                        allocation.setPayment(payment);
                        allocation.setAllocatedAmount(req.getAllocatedAmount());
                        return allocation;
                    })
                    .collect(Collectors.toList());
            allocations.forEach(payment::addAllocation);
        }

        if (request.getAttachmentIds() != null) {
            payment.setAttachments(request.getAttachmentIds());
        }
    }

    public static PaymentRefundEntity toRefundEntity(
            RefundPaymentRequest request,
            PaymentEntity payment,
            JournalEntry journalEntry
    ) {
        PaymentRefundEntity refund = new PaymentRefundEntity();

        refund.setPayment(payment);
        refund.setRefundDate(request.getRefundDate());
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        refund.setReferenceNumber(request.getReferenceNumber());
        refund.setMemo(request.getMemo());
        refund.setRefundJournal(journalEntry);

        return refund;
    }

    public static PaymentDetailsResponse toDetailsResponse(
            PaymentEntity payment,
            List<PaymentAllocationResponse> allocationResponses,
            List<PaymentRefundResponse> refundResponses
    ) {
        PaymentDetailsResponse response = new PaymentDetailsResponse();

        response.setId(payment.getId());
        response.setReceiptNumber(payment.getReceiptNumber());

        if (payment.getCustomer() != null) {
            response.setCustomerId(payment.getCustomer().getId());
            response.setCustomerName(payment.getCustomer().getDisplayName());
            response.setCustomerCode(payment.getCustomer().getCustomerCode());
            response.setCustomerPhone(payment.getCustomer().getPhone());
            response.setCustomerEmail(payment.getCustomer().getEmail());
        }

        response.setPaymentDate(payment.getPaymentDate());
        response.setReceiptDate(payment.getReceiptDate());
        response.setPaymentMethod(payment.getPaymentMethod());

        if (payment.getBankAccount() != null) {
            response.setBankAccountId(payment.getBankAccount().getId());
            response.setBankAccountName(payment.getBankAccount().getCoa_description());
        }

        response.setCurrency(payment.getCurrency());
        response.setExchangeRate(payment.getExchangeRate());
        response.setAmountReceived(payment.getAmountReceived());
        response.setAllocatedAmount(payment.getAllocatedAmount());
        response.setUnallocatedAmount(payment.getUnallocatedAmount());
        response.setReferenceNumber(payment.getReferenceNumber());
        response.setMemo(payment.getMemo());
        response.setStatus(payment.getStatus());
        response.setAttachmentIds(payment.getAttachments());
        response.setAllocations(allocationResponses != null ? allocationResponses : Collections.emptyList());
        response.setRefunds(refundResponses != null ? refundResponses : Collections.emptyList());

        return response;
    }

    public static PaymentListItemResponse toListItemResponse(PaymentEntity payment) {
        PaymentListItemResponse response = new PaymentListItemResponse();

        response.setId(payment.getId());
        response.setReceiptNumber(payment.getReceiptNumber());

        if (payment.getCustomer() != null) {
            response.setCustomerId(payment.getCustomer().getId());
            response.setCustomerName(payment.getCustomer().getDisplayName());
        }

        response.setPaymentDate(payment.getPaymentDate());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setAmountReceived(payment.getAmountReceived());
        response.setAllocatedAmount(payment.getAllocatedAmount());
        response.setUnallocatedAmount(payment.getUnallocatedAmount());
        response.setStatus(payment.getStatus());

        return response;
    }

    public static CreatePaymentResponse toCreateResponse(PaymentEntity payment, String message) {
        CreatePaymentResponse response = new CreatePaymentResponse();

        response.setPaymentId(payment.getId());
        response.setReceiptNumber(payment.getReceiptNumber());
        response.setStatus(payment.getStatus());
        response.setMessage(message);

        return response;
    }

    public static PaymentAllocationResponse toAllocationResponse(
            PaymentAllocationEntity allocation,
            BigDecimal outstandingBefore,
            BigDecimal outstandingAfter
    ) {
        PaymentAllocationResponse response = new PaymentAllocationResponse();

        response.setAllocationId(allocation.getId());
        response.setAllocatedAmount(allocation.getAllocatedAmount());
        response.setOutstandingBefore(outstandingBefore);
        response.setOutstandingAfter(outstandingAfter);

        return response;
    }

    public static PaymentRefundResponse toRefundResponse(PaymentRefundEntity refund) {
        PaymentRefundResponse response = new PaymentRefundResponse();

        response.setRefundId(refund.getId());
        response.setRefundDate(refund.getRefundDate());
        response.setAmount(refund.getAmount());
        response.setReason(refund.getReason());
        response.setReferenceNumber(refund.getReferenceNumber());
        response.setStatus(refund.getStatus());

        return response;
    }

    public static PaymentSummaryResponse toSummaryResponse(PaymentEntity payment) {
        PaymentSummaryResponse response = new PaymentSummaryResponse();

        response.setAmountReceived(payment.getAmountReceived());
        response.setAllocatedAmount(payment.getAllocatedAmount());
        response.setUnallocatedAmount(payment.getUnallocatedAmount());

        return response;
    }
}
