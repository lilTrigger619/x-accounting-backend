package com.unionsg.xaccounting.dto.payroll;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class RecordStatutoryPaymentRequest {
    private Long statutorySchemeId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    /** true settles the employee-withheld portion's liability account, false the employer's. */
    private boolean employeePortion;
}
