package com.unionsg.xaccounting.dto.loan;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Body for lifecycle actions: reverse, default, close, accrue interest, mark missed. Fields not used by an action are ignored. */
@Getter
@Setter
public class LoanActionRequest {
    private String reason;
    private LocalDate date;
    private BigDecimal amount;
    /** Close only: write off what a lent loan still owes. */
    private Boolean writeOff;
}
