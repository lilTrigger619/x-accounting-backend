package com.unionsg.xaccounting.dto.deposit;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepositStatementLineResponse {
    private LocalDate date;
    private Long depositId;
    private String depositNumber;
    private String event;
    private String description;
    private String reference;
    private BigDecimal increase;
    private BigDecimal decrease;
    private BigDecimal balance;
}
