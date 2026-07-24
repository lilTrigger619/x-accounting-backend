package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.dto.payment.AllocatePaymentRequest;
import com.unionsg.xaccounting.dto.payment.PaymentAllocationResponse;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentAllocationService {

    List<PaymentAllocationResponse> allocatePayment(Long paymentId, @Valid AllocatePaymentRequest request);

    void removeAllocation(Long allocationId);

    List<PaymentAllocationResponse> reallocatePayment(Long allocationId, Long newInvoiceId, BigDecimal newAmount);

    List<PaymentAllocationResponse> autoAllocateOldest(Long paymentId);

    List<PaymentAllocationResponse> autoAllocateLargest(Long paymentId);

    void clearAllocations(Long paymentId);

    List<PaymentAllocationResponse> getPaymentAllocations(Long paymentId);
}
