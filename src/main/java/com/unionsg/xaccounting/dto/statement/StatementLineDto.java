package com.unionsg.xaccounting.dto.statement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatementLineDto {

    private LocalDate date;
    private String type;
    private String reference;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
}
