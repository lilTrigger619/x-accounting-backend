package com.unionsg.xaccounting.dto.supplierpayment;

import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSupplierPaymentResponse {

    private Long paymentId;

    private String paymentNumber;

    private SupplierPaymentStatus status;

    private String message;
}
