package com.unionsg.xaccounting.dto.payment;

import com.unionsg.xaccounting.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePaymentResponse {

    private Long paymentId;

    private String receiptNumber;

    private PaymentStatus status;

    private String message;
}

