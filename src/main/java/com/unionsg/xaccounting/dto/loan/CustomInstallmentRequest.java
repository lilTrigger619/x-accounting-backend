package com.unionsg.xaccounting.dto.loan;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One installment of a CUSTOM_SCHEDULE loan. Interest is worked out on the reducing balance when left blank. */
@Getter
@Setter
@NoArgsConstructor
public class CustomInstallmentRequest {
    private LocalDate dueDate;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal fees;

    public CustomInstallmentRequest(LocalDate dueDate, BigDecimal principal, BigDecimal interest, BigDecimal fees) {
        this.dueDate = dueDate;
        this.principal = principal;
        this.interest = interest;
        this.fees = fees;
    }
}
