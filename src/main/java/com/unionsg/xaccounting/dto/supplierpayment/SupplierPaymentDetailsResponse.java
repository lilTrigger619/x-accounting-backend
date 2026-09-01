package com.unionsg.xaccounting.dto.supplierpayment;

import com.unionsg.xaccounting.enums.Currency;
import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SupplierPaymentDetailsResponse {

    private Long id;

    private String paymentNumber;

    private Long supplierId;

    private String supplierName;

    private String supplierCode;

    private String supplierPhone;

    private String supplierEmail;

    private LocalDate paymentDate;

    private PaymentMethod paymentMethod;

    private Long bankAccountId;

    private String bankAccountName;

    private Currency currency;

    private BigDecimal exchangeRate;

    private BigDecimal amountPaid;

    private BigDecimal allocatedAmount;

    private BigDecimal unallocatedAmount;

    private String referenceNumber;

    private String memo;

    private SupplierPaymentStatus status;

    private List<Long> attachmentIds;

    private List<SupplierPaymentAllocationResponse> allocations;
}
