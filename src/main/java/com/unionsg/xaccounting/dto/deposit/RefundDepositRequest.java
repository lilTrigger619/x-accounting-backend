package com.unionsg.xaccounting.dto.deposit;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefundDepositRequest {
    private BigDecimal amount;
    private LocalDate refundDate;
    /** Defaults to the deposit's own bank account, then the mapped default. */
    private Long bankAccountId;
    private String reference;
    private String notes;
}
