package com.unionsg.xaccounting.dto.deposit;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForfeitDepositRequest {
    private BigDecimal amount;
    private LocalDate forfeitDate;
    /** Chart of Accounts primary key; defaults to the type's forfeiture account, then the mapped default. */
    private Long forfeitureAccountId;
    private String reference;
    private String notes;
}
