package com.unionsg.xaccounting.dto.supplierpayment;

import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.SupplierPaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class SupplierPaymentFilterRequest {

    private String search;

    private Long supplierId;

    private PaymentMethod paymentMethod;

    private SupplierPaymentStatus status;

    private Long bankAccountId;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Integer page;

    private Integer size;

    private String sortBy;

    private String sortDirection;
}
