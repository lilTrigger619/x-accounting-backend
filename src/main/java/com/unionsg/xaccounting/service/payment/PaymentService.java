package com.unionsg.xaccounting.service.payment;

import com.unionsg.xaccounting.dto.payment.*;
import com.unionsg.xaccounting.enums.PaymentStatus;
import org.springframework.data.domain.Page;

public interface PaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    CreatePaymentResponse saveDraft(CreateDraftPaymentRequest request);

    CreatePaymentResponse updateDraft(Long id, UpdateDraftPaymentRequest request);

    void deleteDraft(Long id);

    PaymentDetailsResponse getPaymentById(Long id);

    Page<PaymentListItemResponse> getPayments(PaymentFilterRequest filterRequest);

    void changeStatus(Long id, PaymentStatus newStatus);
}
