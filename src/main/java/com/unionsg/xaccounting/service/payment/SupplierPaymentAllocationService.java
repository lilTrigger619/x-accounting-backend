package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.dto.supplierpayment.AllocateSupplierPaymentRequest;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentAllocationResponse;

import java.util.List;

public interface SupplierPaymentAllocationService {

    List<SupplierPaymentAllocationResponse> allocatePayment(Long paymentId, AllocateSupplierPaymentRequest request);

    void removeAllocation(Long allocationId);

    List<SupplierPaymentAllocationResponse> autoAllocateOldest(Long paymentId);

    List<SupplierPaymentAllocationResponse> autoAllocateLargest(Long paymentId);

    void clearAllocations(Long paymentId);

    List<SupplierPaymentAllocationResponse> getPaymentAllocations(Long paymentId);
}
