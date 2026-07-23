package com.unionsg.xaccounting.dto.payment;

import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PaymentDetailsResponse {

    private Long id;

    private String receiptNumber;

    private Long customerId;

    private String customerName;

    private String customerCode;

    private String customerPhone;

    private String customerEmail;

    private LocalDate paymentDate;

    private LocalDate receiptDate;

    private PaymentMethod paymentMethod;

    private Long bankAccountId;

    private String bankAccountName;

    private Currency currency;

    private BigDecimal exchangeRate;

    private BigDecimal amountReceived;

    private BigDecimal allocatedAmount;

    private BigDecimal unallocatedAmount;

    private String referenceNumber;

    private String memo;

    private PaymentStatus status;

    private List<Long> attachmentIds;

    private String createdBy;

    private LocalDateTime createdDate;

    private String lastModifiedBy;

    private LocalDateTime lastModifiedDate;

    private List<PaymentAllocationResponse> allocations;

    private List<PaymentRefundResponse> refunds;
}

