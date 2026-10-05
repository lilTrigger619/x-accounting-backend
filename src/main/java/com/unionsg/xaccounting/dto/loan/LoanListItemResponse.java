package com.unionsg.xaccounting.dto.loan;

import com.unionsg.xaccounting.enums.loan.LoanDirection;
import com.unionsg.xaccounting.enums.loan.LoanInterestMethod;
import com.unionsg.xaccounting.enums.loan.LoanStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class LoanListItemResponse {
    private Long id;
    private String loanNumber;
    private Long loanTypeId;
    private String loanTypeName;
    private LoanDirection direction;
    private String counterpartyName;
    private BigDecimal principalAmount;
    private BigDecimal outstandingPrincipal;
    private BigDecimal interestRate;
    private LoanInterestMethod interestMethod;
    private String currency;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private LocalDate nextDueDate;
    private BigDecimal nextDueAmount;
    private BigDecimal overdueAmount;
    private LoanStatus status;
}
