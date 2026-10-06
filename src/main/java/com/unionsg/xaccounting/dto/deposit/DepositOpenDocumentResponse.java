package com.unionsg.xaccounting.dto.deposit;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An invoice (deposit received) or bill (deposit paid) a deposit can be applied to. */
@Getter
@Setter
public class DepositOpenDocumentResponse {
    private String documentType;
    private Long id;
    private String number;
    private LocalDate date;
    private LocalDate dueDate;
    private String currency;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal balance;
}
