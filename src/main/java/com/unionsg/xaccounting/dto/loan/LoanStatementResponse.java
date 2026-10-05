package com.unionsg.xaccounting.dto.loan;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Chronological statement of a loan from the counterparty's balance point of view: what was
 * advanced, charged, paid and written off, with the running balance owed.
 */
@Getter
@Setter
public class LoanStatementResponse {
    private Long loanId;
    private String loanNumber;
    private String counterpartyName;
    private String currency;
    private String direction;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal totalCharged;
    private BigDecimal totalPaid;
    private List<Entry> entries = new ArrayList<>();

    @Getter
    @Setter
    public static class Entry {
        private LocalDate date;
        /** DISBURSEMENT, FEE, INTEREST_ACCRUAL, PAYMENT, PAYMENT_REVERSAL, WRITE_OFF, LOAN_REVERSAL. */
        private String type;
        private String description;
        private String reference;
        private BigDecimal principal = BigDecimal.ZERO;
        private BigDecimal interest = BigDecimal.ZERO;
        private BigDecimal fees = BigDecimal.ZERO;
        /** Increases what is owed. */
        private BigDecimal charge = BigDecimal.ZERO;
        /** Reduces what is owed. */
        private BigDecimal payment = BigDecimal.ZERO;
        private BigDecimal balance;
        private String journalNumber;
    }
}
