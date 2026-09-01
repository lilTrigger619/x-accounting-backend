package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.dto.supplierpayment.CreateSupplierPaymentRequest;
import com.unionsg.xaccounting.dto.supplierpayment.CreateSupplierPaymentResponse;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentDetailsResponse;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentFilterRequest;
import com.unionsg.xaccounting.dto.supplierpayment.SupplierPaymentListItemResponse;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import org.springframework.data.domain.Page;

public interface SupplierPaymentService {

    CreateSupplierPaymentResponse createPayment(CreateSupplierPaymentRequest request);

    CreateSupplierPaymentResponse saveDraft(CreateSupplierPaymentRequest request);

    CreateSupplierPaymentResponse updateDraft(Long id, CreateSupplierPaymentRequest request);

    void deleteDraft(Long id);

    SupplierPaymentDetailsResponse getPaymentById(Long id);

    Page<SupplierPaymentListItemResponse> getPayments(SupplierPaymentFilterRequest filterRequest);

    void changeStatus(Long id, SupplierPaymentStatus newStatus);
}
