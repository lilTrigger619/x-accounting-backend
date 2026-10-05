package com.unionsg.xaccounting.dto.downpayment;

import com.unionsg.xaccounting.enums.downpayment.DownpaymentType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Create or update (draft only) a downpayment. */
@Data
public class DownpaymentRequest {
    private DownpaymentType type;
    /** Required for CUSTOMER_DOWNPAYMENT. */
    private Long customerId;
    /** Required for SUPPLIER_DOWNPAYMENT. */
    private Long supplierId;
    private LocalDate paymentDate;
    private BigDecimal amount;
    private String currency;
    /** The bank/cash account the money moved through. */
    private Long bankAccountId;
    /** Optional GL account code overriding the mapped downpayment liability/asset account. */
    private String controlAccountCode;
    private String reference;
    private String description;
    /** When true the downpayment is posted straight away instead of being saved as a draft. */
    private Boolean post;
}
