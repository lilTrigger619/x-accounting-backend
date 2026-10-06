package com.unionsg.xaccounting.dto.bankrec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRowResponse {
    private Integer rowNumber;
    /** NEW, DUPLICATE or ERROR. */
    private String outcome;
    private String message;
    private LocalDate transactionDate;
    private LocalDate valueDate;
    private String description;
    private String reference;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal amount;
    private BigDecimal balance;
    private String externalTransactionId;
}
