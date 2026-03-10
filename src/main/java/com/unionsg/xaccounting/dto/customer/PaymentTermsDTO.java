package com.unionsg.xaccounting.dto.customer;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class PaymentTermsDTO {
    private String paymentTermType;
    private BigDecimal creditLimit;
    private String currency;
}
