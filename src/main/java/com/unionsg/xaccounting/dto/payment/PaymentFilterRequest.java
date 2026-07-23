package com.unionsg.xaccounting.dto.payment;

import com.unionsg.xaccounting.enums.PaymentMethod;
import com.unionsg.xaccounting.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class PaymentFilterRequest {

    private String search;

    private Long customerId;

    private PaymentMethod paymentMethod;

    private PaymentStatus status;

    private Long bankAccountId;

    private LocalDate fromDate;

    private LocalDate toDate;

    private Integer page = 0;

    private Integer size = 20;

    private String sortBy = "paymentDate";

    private String sortDirection = "DESC";
}

