package com.unionsg.xaccounting.dto.customer;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentTermsDTO {
    private String paymentTermType;
    private BigDecimal creditLimit;
    private String currency;
}
