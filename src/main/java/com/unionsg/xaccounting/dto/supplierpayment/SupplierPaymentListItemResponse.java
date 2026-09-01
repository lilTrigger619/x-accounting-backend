package com.unionsg.xaccounting.dto.supplierpayment;

import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SupplierPaymentListItemResponse {

    private Long id;

    private String paymentNumber;

    private Long supplierId;

    private String supplierName;

    private LocalDate paymentDate;

    private PaymentMethod paymentMethod;

    private String bankAccountName;

    private BigDecimal amountPaid;

    private BigDecimal allocatedAmount;

    private BigDecimal unallocatedAmount;

    private SupplierPaymentStatus status;
}
