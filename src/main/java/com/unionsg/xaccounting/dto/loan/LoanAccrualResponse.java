package com.unionsg.xaccounting.dto.loan;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LoanAccrualResponse {
    private Long id;
    private LocalDate accrualDate;
    private BigDecimal amount;
    private String memo;
    private Long journalId;
    private String journalNumber;
    private boolean reversed;
}
