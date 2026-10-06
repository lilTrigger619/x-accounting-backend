package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanDirection;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LoanDashboardResponse {
    private LocalDate asOf;
    private LocalDate fromDate;
    private LocalDate toDate;
    /** One set of totals per loan currency; amounts are never added across currencies. */
    private List<CurrencyTotals> totals = new ArrayList<>();
    private List<DuePayment> upcomingPayments = new ArrayList<>();
    private List<DuePayment> overduePayments = new ArrayList<>();
    private long activeLoans;
    private long draftLoans;
    private long defaultedLoans;

    @Getter
    @Setter
    public static class CurrencyTotals {
        private String currency;
        private BigDecimal totalBorrowed = BigDecimal.ZERO;
        private BigDecimal totalLent = BigDecimal.ZERO;
        private BigDecimal outstandingBorrowed = BigDecimal.ZERO;
        private BigDecimal outstandingLent = BigDecimal.ZERO;
        /** Interest on installments due by today and not yet paid, payable by us. */
        private BigDecimal interestDuePayable = BigDecimal.ZERO;
        /** Interest on installments due by today and not yet paid, owed to us. */
        private BigDecimal interestDueReceivable = BigDecimal.ZERO;
        private BigDecimal interestIncome = BigDecimal.ZERO;
        private BigDecimal interestExpense = BigDecimal.ZERO;
        private BigDecimal upcomingAmount = BigDecimal.ZERO;
        private BigDecimal overdueAmount = BigDecimal.ZERO;
    }

    @Getter
    @Setter
    public static class DuePayment {
        private Long loanId;
        private String loanNumber;
        private String counterpartyName;
        private LoanDirection direction;
        private String currency;
        private Integer installmentNumber;
        private LocalDate dueDate;
        private BigDecimal amountDue;
        private long daysOverdue;
        private boolean missed;
    }
}
