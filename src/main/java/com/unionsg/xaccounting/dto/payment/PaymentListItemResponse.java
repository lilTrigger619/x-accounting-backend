package com.unionsg.xaccounting.dto.payment;

import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class PaymentListItemResponse {

    private Long id;

    private String receiptNumber;

    private Long customerId;

    private String customerName;

    private LocalDate paymentDate;

    private PaymentMethod paymentMethod;

    private String bankAccountName;

    private BigDecimal amountReceived;

    private BigDecimal allocatedAmount;

    private BigDecimal unallocatedAmount;

    private PaymentStatus status;
}

